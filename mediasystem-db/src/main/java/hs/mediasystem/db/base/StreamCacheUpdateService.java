package hs.mediasystem.db.base;

import hs.ddif.annotations.Opt;
import hs.mediasystem.db.base.StreamCacheUpdater.EnrichmentException;
import hs.mediasystem.db.base.StreamCacheUpdater.Type;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.WorkIdCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/*
 * Unfortunately, this class is a bit complicated.
 *
 * The most important scenario that this needs to handle is the correct enrichment of child items that
 * require their parent's data to do their job. This runs into a couple of problems:
 *
 * 1) If parent is enriched more recently, child items should be refreshed
 *    -> This needs to be done without updating the enrich time on the parent data as
 *       this could cause a refresh loop:
 *        - Child 1 is refreshed, parent is refreshed
 *        - Child 2 which wasn't refreshed is now older than parent
 *        - Child 2 is refreshed and parent is also refreshed
 *        - Child 1 which wasn't refreshed in the last step is now older than parent
 *          etc.
 *
 * 2) If a child item has no match then enrichment should be retried. This will also enrich the parent.
 * Some children however ALWAYS fail (there is never a match), so in order to prevent a refresh loop
 * enrichment should only be retried if the child has a discovery time or modification time that is
 * after the parent's last enrich time.
 *
 * 3) When using #reidentify, only the parent gets refreshed. Because of #1 this will now
 * trigger a refresh of all children as well.
 *
 */

@Singleton
public class StreamCacheUpdateService {
  private static final Logger LOGGER = Logger.getLogger(StreamCacheUpdateService.class.getName());

  @Inject private LocalMediaIdentificationService identificationService;
  @Inject private DatabaseStreamStore streamStore;
  @Inject private DatabaseDescriptorStore descriptorStore;
  @Inject private StreamCacheUpdater streamCacheUpdater;

  @Inject @Opt @Named("server.meta-data-updater.initial-delay") private long initialDelay = 15;

  private final AutoReentrantLock storeConsistencyLock = new AutoReentrantLock();  // Used to sync actions of this class

  @PostConstruct
  private void postConstruct() {
    initializePeriodicEnrichThread();
  }

  public synchronized void update(int importSourceId, List<Streamable> results) {
    Map<StreamID, Streamable> existingStreams = streamStore.findByImportSourceId(importSourceId);  // Returns all active streams (not deleted)

    for(Streamable found : results) {
      try {
        Streamable existing = existingStreams.remove(found.getId());

        update(found, existing);
      }
      catch(Throwable t) {
        LOGGER.severe("Exception while updating: " + found + ": " + Throwables.formatAsOneLine(t));
      }
    }

    /*
     * After updating, existingStreams will only contain the streams that were
     * not found during the last scan.  These will be removed.
     */

    try(Key key = storeConsistencyLock.lock()) {
      existingStreams.entrySet().stream()
        .peek(e -> LOGGER.finer("Existing Stream deleted: " + e.getValue()))
        .map(Map.Entry::getKey)
        .forEach(streamStore::remove);
    }
  }

  public Optional<CompletableFuture<MediaIdentification>> reidentifyStream(StreamID streamId) {
    LOGGER.info("Triggered manual reidentificating of: " + streamId);

    return streamStore.findStream(streamId).map(s -> asyncEnrich(Type.HIGH_UNCACHED, s));
  }

  private void update(Streamable found, Streamable existing) {
    if(existing == null || !found.equals(existing)) {
      try(Key key = storeConsistencyLock.lock()) {
        streamStore.put(found);  // Adds as new or modify it
      }

      LOGGER.finer((existing == null ? "New stream found: " : "Existing stream modified: ") + found);

      asyncEnrich(Type.HIGH_CACHED, found);  // TODO this could result in the enrich failing if the item is new, belongs to a serie and the serie data is old; next pass should fix this though (see else)
    }
    else {
      Optional<Identification> identification = streamStore.findIdentification(existing.getId());

      // For streams with parents:
      streamStore.findParentId(existing.getId()).flatMap(streamStore::findStream).ifPresent(parent -> {
        streamStore.findLastEnrichTime(parent.getId()).ifPresent(parentLastEnrichTime -> {

          // Refresh parent if stream has no identification and its discovery time is newer than parent's last enrich time:
          if(identification.isEmpty()) {
            streamStore.findDiscoveryTime(existing.getId()).ifPresent(discoveryTime -> {
              if(discoveryTime.isAfter(parentLastEnrichTime)) {
                LOGGER.warning("Existing stream has no identification and was discovered later than parent (" + parent + ") -> refetching parent: " + parent);

                asyncEnrich(Type.HIGH_CACHED, parent);
              }
            });
          }

          // Refresh existing stream if parent has a newer enrich time:
          streamStore.findLastEnrichTime(existing.getId()).ifPresent(lastEnrichTime -> {
            if(parentLastEnrichTime.isAfter(lastEnrichTime)) {
              LOGGER.warning("Existing stream with time " + lastEnrichTime + " has updated parent with time " + parentLastEnrichTime + " (" + parent + ") -> refetching: " + found);

              asyncEnrich(Type.HIGH_UNCACHED, existing);
            }
          });
        });
      });

      /*
       * Checks to see if items are complete; this only occurs if normal operations were interrupted.
       * Cache use should not be forced here as partial data cached may not be in sync with new data
       * that will be fetched.
       */

      identification.stream().map(Identification::getWorkIds).flatMap(Collection::stream).forEach(wid -> {
        if(identificationService.isQueryServiceAvailable(wid.getDataSource(), wid.getType())) {
          WorkDescriptor workDescriptor = descriptorStore.find(wid).orElse(null);

          if(workDescriptor == null) {
            // One or more descriptors are missing, enrich:
            LOGGER.warning("Existing stream is missing descriptors in cache (" + wid + ") -> refetching: " + found);

            asyncEnrich(Type.HIGH_UNCACHED, found);
          }
          else if(workDescriptor instanceof Production production) {
            WorkId collectionId = production.getCollectionId().orElse(null);

            if(collectionId != null) {
              WorkIdCollection workIdCollection = (WorkIdCollection)descriptorStore.find(collectionId).orElse(null);

              if(workIdCollection == null) {
                LOGGER.warning("Existing stream is missing collection data in cache (" + collectionId + ") -> refetching: " + found);

                asyncEnrich(Type.HIGH_UNCACHED, found);
              }
              else {
                for(WorkId itemId : workIdCollection.getItems()) {
                  if(descriptorStore.find(itemId).orElse(null) == null) {
                    LOGGER.warning("Existing stream is missing collection items in cache (" + itemId + " is missing, out of " + workIdCollection.getItems() + " from " + collectionId + ") -> refetching: " + found);

                    asyncEnrich(Type.HIGH_UNCACHED, found);
                    break;
                  }
                }
              }
            }
          }
        }
      });
    }
  }

  private CompletableFuture<MediaIdentification> asyncEnrich(Type type, Streamable streamable) {
    return streamCacheUpdater.asyncEnrich(type, streamable, (sid, pmi) -> enrichTask(sid, pmi, streamable.getId()));
  }

  private void triggerInitialEnriches() {
    Set<Streamable> unenrichedStreams = streamStore.findUnenrichedStreams();

    LOGGER.fine("Triggering first time enrich of " + unenrichedStreams.size() + " streams");

    unenrichedStreams.forEach(s -> asyncEnrich(Type.HIGH_UNCACHED, s));
  }

  private void initializePeriodicEnrichThread() {
    Thread reidentifyThread = new Thread(() -> {
      try {
        Thread.sleep(initialDelay * 1000);  // Initial delay, to avoid triggering immediately on restart
      }
      catch(InterruptedException e1) {
        // Ignore
      }

      triggerInitialEnriches();

      for(;;) {
        streamStore.findStreamsNeedingRefresh(500).stream().forEach(s -> asyncEnrich(Type.LOW_UNCACHED, s));

        try {
          Thread.sleep(300000);
        }
        catch(InterruptedException e) {
          // Ignore
        }
      }
    });

    reidentifyThread.setDaemon(true);
    reidentifyThread.setPriority(Thread.NORM_PRIORITY - 2);
    reidentifyThread.setName("StreamCacheUpdateService-Reidentifier");
    reidentifyThread.start();
  }

  private MediaIdentification enrichTask(StreamID streamId, MediaIdentification pmi, StreamID triggerStreamId) {
    WorkDescriptor parent = pmi == null ? null : pmi.getDescriptor();

    try(Key key = storeConsistencyLock.lock()) {
      Streamable streamable = streamStore.findStream(streamId).orElseThrow(() -> new IllegalStateException("Stream with id " + streamId + " no longer available"));   // As tasks can take a while before they start, fetch latest state from StreamStore first
      List<String> dataSourceNames = parent == null ? streamStore.findStreamSource(streamId).dataSourceNames() : List.of(parent.getId().getDataSource().getName());

      key.earlyUnlock();

      try {
        Exception cause = null;

        for(String sourceName : dataSourceNames) {
          try {
            MediaIdentification result = identificationService.identify(streamable, parent, sourceName);

            fetchAndStoreCollectionDescriptors(result);
            updateCacheWithIdentification(result);

            return result;   // if identification was successful, no need to try next data source
          }
          catch(IOException e) {
            if(cause == null) {
              cause = e;
            }
            else {
              cause.addSuppressed(e);
            }
          }
        }

        throw new EnrichmentException(streamable, dataSourceNames, cause);
      }
      finally {
        if(streamId.equals(triggerStreamId)) {

          /*
           * Only mark enriched when the triggered stream matches the enriched stream. This is
           * to prevent enriches of parents (which are required to enrich children) triggering
           * enriches again for their children, as children must have a newer enrich time than
           * their parent.
           */

          streamStore.markEnriched(streamable.getId());  // Prevent further enrich attempts, successful or not
        }
      }
    }
  }

  /**
   * Fetches all descriptors that are part of a collection, including descriptors that
   * may not be in the local collection, and stores them in the descriptor store for
   * fast access.
   *
   * @param mediaIdentification a {@link MediaIdentification} result to check for {@link WorkId}s of type COLLECTION.
   */
  private void fetchAndStoreCollectionDescriptors(MediaIdentification mediaIdentification) {
    // Production contains related Collection id which can be queried to get WorkIdCollection which in turn are each queried to get further Productions
    WorkDescriptor descriptor = mediaIdentification.getDescriptor();

    if(descriptor instanceof Production production) {
      production.getRelatedWorks().stream()
        .filter(wid -> wid.getType().equals(MediaType.COLLECTION))  // After this filtering, stream consists of Collection type ids
        .filter(wid -> wid.getDataSource().getName().equals(production.getId().getDataSource().getName()))  // Only Collection type ids of same data source as production that contained it
        .forEach(this::fetchAndStoreCollectionItems);
    }
  }

  private void fetchAndStoreCollectionItems(WorkId collectionId) {
    try {
      WorkIdCollection descriptor = (WorkIdCollection)identificationService.query(collectionId);

      for(WorkId id : descriptor.getItems()) {
        try {
          descriptorStore.add(identificationService.query(id));
        }
        catch(Exception e) {
          LOGGER.warning("Exception while fetching descriptor for " + id + " in collection " + collectionId + ": " + Throwables.formatAsOneLine(e));
        }
      }

      descriptorStore.add(descriptor);
    }
    catch(Exception e) {
      LOGGER.warning("Exception while fetching collection descriptor for " + collectionId + ": " + Throwables.formatAsOneLine(e));
    }
  }

  private void updateCacheWithIdentification(MediaIdentification mediaIdentification) {
    try(Key key = storeConsistencyLock.lock()) {
      StreamID streamId = mediaIdentification.getStreamable().getId();

      // Store descriptors in descriptor store:
      if(mediaIdentification.getDescriptor() != null) {
        descriptorStore.add(mediaIdentification.getDescriptor());
      }

      // Store identification with stream:
      streamStore.putIdentification(streamId, mediaIdentification.getIdentification());
    }
  }
}

package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.mediamanager.StreamSource;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamCacheUpdateService {
  private static final Logger LOGGER = Logger.getLogger(StreamCacheUpdateService.class.getName());
  private static final LinkedBlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<>();
  private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, QUEUE, new NamedThreadFactory("StreamCacheUpdateSvc", Thread.NORM_PRIORITY - 2, true));
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");
  private static final MediaType COLLECTION_MEDIA_TYPE = MediaType.of("COLLECTION");

  @Inject private LocalMediaIdentificationService identificationService;
  @Inject private DatabaseStreamStore streamStore;
  @Inject private DatabaseDescriptorStore descriptorStore;

  private final AutoReentrantLock storeConsistencyLock = new AutoReentrantLock();  // Used to sync actions of this class

  @PostConstruct
  private void postConstruct() {
    triggerInitialEnriches();
    initializePeriodicEnrichThread();
  }

  private void triggerInitialEnriches() {
    Set<StreamID> unenrichedStreams = streamStore.findUnenrichedStreams();

    LOGGER.fine("Triggering first time enrich of " + unenrichedStreams.size() + " streams");

    unenrichedStreams.forEach(sid -> asyncEnrichMediaStream(sid));
  }

  private void initializePeriodicEnrichThread() {
    Thread reidentifyThread = new Thread(() -> {
      try {
        Thread.sleep(300000);  // Initial delay, to avoid triggering immediately on restart
      }
      catch(InterruptedException e1) {
        // Ignore
      }

      for(;;) {
        if(EXECUTOR.getQueue().isEmpty()) {
          streamStore.findStreamsNeedingRefresh(40).stream()
            .forEach(sid -> asyncEnrichMediaStream(sid));
        }

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

  private void asyncEnrichMediaStream(StreamID streamId) {
    WORKLOAD.start();
    EXECUTOR.execute(() -> {
      try {
        try(Key key = storeConsistencyLock.lock()) {
          streamStore.findStream(streamId).ifPresent(stream -> {
            StreamSource source = streamStore.findStreamSource(streamId);

            key.earlyUnlock();

            enrich(source, stream);
          });
        }
      }
      finally {
        WORKLOAD.complete();
      }
    });
  }

  /**
   * Fetches all descriptors that are part of a collection, including descriptors that
   * may not be in the local collection, and stores them in the descriptor store for
   * fast access.
   *
   * @param mediaIdentification a {@link MediaIdentification} result to check for {@link Identifier}s of type COLLECTION.
   */
  private void fetchAndStoreCollectionDescriptors(MediaIdentification mediaIdentification) {
    // Production contains related Collection identifier which can be queried to get IdentifierCollection which in turn are each queried to get further Productions
    MediaDescriptor descriptor = mediaIdentification.getDescriptor();

    if(descriptor instanceof Production) {
      Production production = (Production)descriptor;

      production.getRelatedIdentifiers().stream()
        .filter(identifier -> identifier.getDataSource().getType().equals(COLLECTION_MEDIA_TYPE))  // After this filtering, stream consists of Collection type identifiers
        .filter(identifier -> identifier.getDataSource().getName().equals(production.getIdentifier().getDataSource().getName()))  // Only Collection type identifiers of same data source as production that contained it
        .forEach(this::fetchAndStoreCollectionItems);
    }
  }

  private void fetchAndStoreCollectionItems(Identifier collectionIdentifier) {
    try {
      IdentifierCollection descriptor = (IdentifierCollection)identificationService.query(collectionIdentifier);

      fetchAndStoreCollectionItems(descriptor);
      descriptorStore.add(descriptor);
    }
    catch(Exception e) {
      LOGGER.warning("Exception while fetching collection descriptor for " + collectionIdentifier + ": " + Throwables.formatAsOneLine(e));
    }
  }

  private void fetchAndStoreCollectionItems(IdentifierCollection identifierCollection) {
    for(Identifier identifier : identifierCollection.getItems()) {
      try {
        descriptorStore.add(identificationService.query(identifier));
      }
      catch(Exception e) {
        LOGGER.warning("Exception while fetching descriptor for " + identifier + ": " + Throwables.formatAsOneLine(e));
      }
    }
  }

  public synchronized void update(long importSourceId, List<Exceptional<List<BasicStream>>> rootResults) {
    for(int rootResultIdx = 0; rootResultIdx < rootResults.size(); rootResultIdx++) {
      Exceptional<List<BasicStream>> rootResult = rootResults.get(rootResultIdx);

      if(rootResult.isPresent()) {
        int scannerAndRootId = (int)importSourceId + rootResultIdx * 65536;
        Map<StreamID, BasicStream> existingStreams = streamStore.findByImportSourceId(scannerAndRootId);  // Returns all active streams (not deleted)

        for(BasicStream scannedStream : rootResult.get()) {
          try {
            BasicStream existingStream = existingStreams.remove(scannedStream.getId());

            if(existingStream == null || !scannedStream.equals(existingStream)) {
              try(Key key = storeConsistencyLock.lock()) {
                streamStore.put(scannerAndRootId, scannedStream);  // Adds as new or modify it
              }

              LOGGER.finer((existingStream == null ? "New stream found: " : "Existing stream modified: ") + scannedStream);

              asyncEnrichMediaStream(scannedStream.getId());
            }
            else {  // Check if descriptor store contains the relevant descriptors
              streamStore.findIdentification(existingStream.getId()).map(Identification::getIdentifier).ifPresent(identifier -> {
                if(identificationService.isQueryServiceAvailable(identifier.getDataSource())) {
                  MediaDescriptor mediaDescriptor = descriptorStore.find(identifier).orElse(null);

                  if(mediaDescriptor == null) {
                    // One or more descriptors are missing, enrich:
                    LOGGER.warning("Existing stream is missing descriptors in cache (" + identifier + ") -> refetching: " + scannedStream);

                    asyncEnrichMediaStream(scannedStream.getId());
                  }
                  else if(mediaDescriptor instanceof Production) {
                    Production production = (Production)mediaDescriptor;
                    Identifier collectionIdentifier = production.getCollectionIdentifier().orElse(null);

                    if(collectionIdentifier != null) {
                      IdentifierCollection identifierCollection = (IdentifierCollection)descriptorStore.find(collectionIdentifier).orElse(null);

                      if(identifierCollection == null) {
                        LOGGER.warning("Existing stream is missing collection data in cache (" + collectionIdentifier + ") -> refetching: " + scannedStream);

                        asyncEnrichMediaStream(scannedStream.getId());
                      }
                      else {
                        for(Identifier collectionItemIdentifier : identifierCollection.getItems()) {
                          if(descriptorStore.find(collectionItemIdentifier).orElse(null) == null) {
                            LOGGER.warning("Existing stream is missing collection items in cache (" + collectionItemIdentifier + " is missing, out of " + identifierCollection.getItems() + " from " + collectionIdentifier + ") -> refetching: " + scannedStream);

                            asyncEnrichMediaStream(scannedStream.getId());
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
          catch(Throwable t) {
            LOGGER.severe("Exception while updating: " + scannedStream + ": " + Throwables.formatAsOneLine(t));
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
    }
  }

  public Optional<MediaIdentification> reidentifyStream(StreamID streamId) {
    try(Key key = storeConsistencyLock.lock()) {
      BasicStream stream = streamStore.findStream(streamId).orElse(null);

      if(stream == null) {
        return null;
      }

      StreamSource source = streamStore.findStreamSource(streamId);

      key.earlyUnlock();

      return enrich(source, stream);
    }
  }

  private Optional<MediaIdentification> enrich(StreamSource source, BasicStream stream) {
    for(String sourceName : source.getDataSourceNames()) {
      try {
        MediaIdentification result = identificationService.identify(stream, sourceName);

        if(result.getIdentifications().isEmpty()) {
          LOGGER.info("Enrichment [" + sourceName + "] returned no matches for " + stream + ":\n - " + result.getIdentifications() + " -> " + result.getDescriptor());
          continue;
        }

        LOGGER.info("Enrichment [" + sourceName + "] succeeded for " + stream + ":\n - " + result.getIdentifications() + " -> " + result.getDescriptor());

        fetchAndStoreCollectionDescriptors(result);
        updateCacheWithIdentification(result);

        return Optional.of(result);   // if identification was succesful, no need to try next data source
      }
      catch(Exception e) {
        LOGGER.warning("Enrichment [" + sourceName + "] failed for " + stream + ":\n - " + Throwables.formatAsOneLine(e));
      }
    }

    return Optional.empty();
  }

  private void updateCacheWithIdentification(MediaIdentification mediaIdentification) {
    try(Key key = storeConsistencyLock.lock()) {
      StreamID streamId = mediaIdentification.getStream().getId();

      // Store identifiers with stream:
      streamStore.putIdentification(streamId, mediaIdentification.getIdentifications().get(streamId));

      // Store descriptors in descriptor store:
      if(mediaIdentification.getDescriptor() != null) {
        descriptorStore.add(mediaIdentification.getDescriptor());
      }

      // Mark enriched
      streamStore.markEnriched(streamId);
    }
  }
}

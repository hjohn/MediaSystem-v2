package hs.mediasystem.db.base;

import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.LocalMediaIdentificationService;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamCacheUpdateService {
  private static final Logger LOGGER = Logger.getLogger(StreamCacheUpdateService.class.getName());
  private static final Map<StreamID, CompletableFuture<MediaIdentification>> RECENT_IDENTIFICATIONS = new ConcurrentHashMap<>();
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");
  private static final MediaType COLLECTION_MEDIA_TYPE = MediaType.of("COLLECTION");

  @Inject private LocalMediaIdentificationService identificationService;
  @Inject private DatabaseStreamStore streamStore;
  @Inject private DatabaseDescriptorStore descriptorStore;
  @Inject private DatabaseResponseCache responseCache;

  private final AutoReentrantLock storeConsistencyLock = new AutoReentrantLock();  // Used to sync actions of this class
  private final Executor forceCacheUseExecutor = createExecutor("StreamCacheUS-cached", true);  // forces cache use for any requests done
  private final Executor normalCachingExecutor = createExecutor("StreamCacheUS-refresh", false);  // normal cache use for any requests done
  private final Executor delayedExecutor = CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES, forceCacheUseExecutor);

  @PostConstruct
  private void postConstruct() {
    triggerInitialEnriches();
    initializePeriodicEnrichThread();
  }

  private Executor createExecutor(String name, boolean forceCacheUse) {
    Executor executor = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory(name, Thread.NORM_PRIORITY - 2, true));

    return r -> executor.execute(() -> {
      responseCache.currentThreadForceCacheUse(forceCacheUse);
      r.run();
    });
  }

  private void triggerInitialEnriches() {
    Set<Streamable> unenrichedStreams = streamStore.findUnenrichedStreams();

    LOGGER.fine("Triggering first time enrich of " + unenrichedStreams.size() + " streams");

    unenrichedStreams.forEach(this::asyncEnrich);
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
        if(RECENT_IDENTIFICATIONS.size() < 10) {
          streamStore.findStreamsNeedingRefresh(40).stream().forEach(s -> asyncEnrich(s, normalCachingExecutor));
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


  /**
   * Asynchronously enriches a {@link Streamable}, if necessary enriching a parent as well.  If the
   * enrichment is already queued or recently completed, this call returns the relevant future (or
   * completed) result.<p>
   *
   * Makes use of a recent identifications list that contains in progress or recently completed
   * identifications.  A child enrichment is linked to its parent and will be completed as soon as
   * its parent completes.
   *
   * @param streamable a Streamable to identify, cannot be null
   * @param executor the {@link Executor} to use, cannot be null
   * @return the (future) result, never null
   */
  private CompletableFuture<MediaIdentification> asyncEnrich(Streamable streamable, Executor executor) {
    StreamID id = streamable.getId();
    CompletableFuture<MediaIdentification> activeIdentification = RECENT_IDENTIFICATIONS.get(id);

    if(activeIdentification != null) {
      return activeIdentification;
    }

    // this code may modify RECENT_IDENTIFICATIONS map, so compute it first before modifying it ourselves:
    activeIdentification = streamable.getParentId()
      .map(pid -> createChildTask(getParentFuture(pid, executor), id, executor))
      .orElseGet(() -> createTask(id, executor));

    RECENT_IDENTIFICATIONS.put(id, activeIdentification);

    return activeIdentification;
  }

  private CompletableFuture<MediaIdentification> asyncEnrich(Streamable streamable) {
    return asyncEnrich(streamable, forceCacheUseExecutor);
  }

  private CompletableFuture<MediaIdentification> getParentFuture(StreamID pid, Executor executor) {
    return RECENT_IDENTIFICATIONS.computeIfAbsent(pid, k -> createTask(k, executor));
  }

  private CompletableFuture<MediaIdentification> createTask(StreamID streamId, Executor executor) {
    WORKLOAD.start();

    return addFinalStages(CompletableFuture.supplyAsync(() -> enrichTask(streamId, null), executor), streamId);
  }

  private CompletableFuture<MediaIdentification> createChildTask(CompletableFuture<MediaIdentification> parentStage, StreamID streamId, Executor executor) {
    WORKLOAD.start();

    return addFinalStages(parentStage.thenApplyAsync(mi -> enrichTask(streamId, mi.getDescriptor()), executor), streamId);
  }

  // Add final stages to newly created futures only; don't call this on futures found in queue as they would get these final stages added again!
  private CompletableFuture<MediaIdentification> addFinalStages(CompletableFuture<MediaIdentification> cf, StreamID streamId) {
    cf.whenComplete((mi, t) -> log(mi, t, streamId))
      .whenComplete((v, ex) -> WORKLOAD.complete())
      .thenRunAsync(() -> RECENT_IDENTIFICATIONS.remove(streamId), delayedExecutor);

    return cf;  // Purposely returning original cf here
  }

  private static void log(MediaIdentification mi, Throwable t, StreamID streamId) {
    if(mi != null) {
      LOGGER.info("Enrichment of " + streamId + " [" + mi.getIdentification().getPrimaryIdentifier().getDataSource() + "] succeeded for " + mi.getStreamable() + ":\n - " + mi.getIdentification() + (mi.getDescriptor() == null ? "" : " -> " + mi.getDescriptor()));
    }
    else if(t instanceof EnrichmentException) {
      EnrichmentException e = (EnrichmentException)t;

      LOGGER.warning("Enrichment of " + streamId + " with data sources " + e.getDataSourceNames() + " failed for " + e.getStreamable() + ":\n - " + Throwables.formatAsOneLine(e));
    }
    else if(t instanceof CompletionException) {
      LOGGER.warning("Enrichment of " + streamId + " failed: " + Throwables.formatAsOneLine(t.getCause()));
    }
    else {
      LOGGER.log(Level.SEVERE, "Enrichment of " + streamId + " failed", t);
    }
  }

  private MediaIdentification enrichTask(StreamID streamId, MediaDescriptor parent) {
    try(Key key = storeConsistencyLock.lock()) {
      Streamable streamable = streamStore.findStream(streamId).orElseThrow(() -> new IllegalStateException("Stream with id " + streamId + " no longer available"));   // As tasks can take a while before they start, fetch latest state from StreamStore first
      List<String> dataSourceNames = parent == null ? streamStore.findStreamSource(streamId).getDataSourceNames() : List.of(parent.getIdentifier().getDataSource().getName());

      key.earlyUnlock();

      return enrich(dataSourceNames, streamable, parent);
    }
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

  public synchronized void update(int importSourceId, List<Streamable> results) {
    Map<StreamID, Streamable> existingStreams = streamStore.findByImportSourceId(importSourceId);  // Returns all active streams (not deleted)

    for(Streamable found : results) {
      try {
        Streamable existing = existingStreams.remove(found.getId());

        if(existing == null || !found.equals(existing)) {
          try(Key key = storeConsistencyLock.lock()) {
            streamStore.put(found);  // Adds as new or modify it
          }

          LOGGER.finer((existing == null ? "New stream found: " : "Existing stream modified: ") + found);

          asyncEnrich(found);
        }
        else {
          // Check if descriptor store contains the relevant descriptors:
          streamStore.findIdentification(existing.getId()).stream().map(Identification::getIdentifiers).flatMap(Collection::stream).forEach(identifier -> {
            if(identificationService.isQueryServiceAvailable(identifier.getDataSource())) {
              MediaDescriptor mediaDescriptor = descriptorStore.find(identifier).orElse(null);

              if(mediaDescriptor == null) {
                // One or more descriptors are missing, enrich:
                LOGGER.warning("Existing stream is missing descriptors in cache (" + identifier + ") -> refetching: " + found);

                asyncEnrich(found);
              }
              else if(mediaDescriptor instanceof Production) {
                Production production = (Production)mediaDescriptor;
                Identifier collectionIdentifier = production.getCollectionIdentifier().orElse(null);

                if(collectionIdentifier != null) {
                  IdentifierCollection identifierCollection = (IdentifierCollection)descriptorStore.find(collectionIdentifier).orElse(null);

                  if(identifierCollection == null) {
                    LOGGER.warning("Existing stream is missing collection data in cache (" + collectionIdentifier + ") -> refetching: " + found);

                    asyncEnrich(found);
                  }
                  else {
                    for(Identifier collectionItemIdentifier : identifierCollection.getItems()) {
                      if(descriptorStore.find(collectionItemIdentifier).orElse(null) == null) {
                        LOGGER.warning("Existing stream is missing collection items in cache (" + collectionItemIdentifier + " is missing, out of " + identifierCollection.getItems() + " from " + collectionIdentifier + ") -> refetching: " + found);

                        asyncEnrich(found);
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
    try(Key key = storeConsistencyLock.lock()) {
      return streamStore.findStream(streamId).map(s -> asyncEnrich(s, normalCachingExecutor));
    }
  }

  private MediaIdentification enrich(List<String> dataSourceNames, Streamable streamable, MediaDescriptor parent) throws EnrichmentException {
    try {
      Exception cause = null;

      for(String sourceName : dataSourceNames) {
        try {
          MediaIdentification result = identificationService.identify(streamable, parent, sourceName);

          fetchAndStoreCollectionDescriptors(result);
          updateCacheWithIdentification(result);

          return result;   // if identification was succesful, no need to try next data source
        }
        catch(Exception e) {
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
      streamStore.markEnriched(streamable.getId());  // Prevent further enrich attempts, succesful or not
    }
  }

  static class EnrichmentException extends RuntimeException {
    private final Streamable streamable;
    private final List<String> dataSourceNames;

    public EnrichmentException(Streamable streamable, List<String> dataSourceNames, Throwable cause) {
      super(cause);

      this.streamable = streamable;
      this.dataSourceNames = dataSourceNames;
    }

    public Streamable getStreamable() {
      return streamable;
    }

    public List<String> getDataSourceNames() {
      return dataSourceNames;
    }
  }

  private void updateCacheWithIdentification(MediaIdentification mediaIdentification) {
    try(Key key = storeConsistencyLock.lock()) {
      StreamID streamId = mediaIdentification.getStreamable().getId();

      // Store identifiers with stream:
      streamStore.putIdentification(streamId, mediaIdentification.getIdentification());

      // Store descriptors in descriptor store:
      if(mediaIdentification.getDescriptor() != null) {
        descriptorStore.add(mediaIdentification.getDescriptor());
      }
    }
  }
}

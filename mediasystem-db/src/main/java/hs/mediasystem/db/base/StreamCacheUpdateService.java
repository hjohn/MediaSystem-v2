package hs.mediasystem.db.base;

import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
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
import hs.mediasystem.util.Exceptional;
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
  private static final Map<ContentID, CompletableFuture<MediaIdentification>> RECENT_IDENTIFICATIONS = new ConcurrentHashMap<>();
  private static final Executor FORCE_CACHE_USE_EXECUTOR = createExecutor("StreamCacheUS-cached", true);  // forces cache use for any requests done
  private static final Executor NORMAL_CACHING_EXECUTOR = createExecutor("StreamCacheUS-refresh", false);  // normal cache use for any requests done
  private static final Executor DELAYED_EXECUTOR = CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES, FORCE_CACHE_USE_EXECUTOR);
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");
  private static final MediaType COLLECTION_MEDIA_TYPE = MediaType.of("COLLECTION");

  private static Executor createExecutor(String name, boolean forceCacheUse) {
    Executor executor = new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory(name, Thread.NORM_PRIORITY - 2, true));

    return r -> executor.execute(() -> {
      DatabaseResponseCache.forceCacheUse(forceCacheUse);
      r.run();
    });
  }

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
          streamStore.findStreamsNeedingRefresh(40).stream().forEach(s -> asyncEnrich(s, NORMAL_CACHING_EXECUTOR));
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
    ContentID id = streamable.getId();
    CompletableFuture<MediaIdentification> activeIdentification = RECENT_IDENTIFICATIONS.get(id);

    if(activeIdentification != null) {
      return activeIdentification;
    }

    // this code may modify RECENT_IDENTIFICATIONS map, so compute it first before modifying it ourselves:
    activeIdentification = streamable.getParentContentId()
      .map(pid -> createChildTask(getParentFuture(pid, executor), id, executor))
      .orElseGet(() -> createTask(id, executor));

    RECENT_IDENTIFICATIONS.put(id, activeIdentification);

    return activeIdentification;
  }

  private CompletableFuture<MediaIdentification> asyncEnrich(Streamable streamable) {
    return asyncEnrich(streamable, FORCE_CACHE_USE_EXECUTOR);
  }

  private CompletableFuture<MediaIdentification> getParentFuture(ContentID pid, Executor executor) {
    return RECENT_IDENTIFICATIONS.computeIfAbsent(pid, k -> createTask(k, executor));
  }

  private CompletableFuture<MediaIdentification> createTask(ContentID contentId, Executor executor) {
    WORKLOAD.start();

    return addFinalStages(CompletableFuture.supplyAsync(() -> enrichTask(contentId, null), executor), contentId);
  }

  private CompletableFuture<MediaIdentification> createChildTask(CompletableFuture<MediaIdentification> parentStage, ContentID contentId, Executor executor) {
    WORKLOAD.start();

    return addFinalStages(parentStage.thenApplyAsync(mi -> enrichTask(contentId, mi.getDescriptor()), executor), contentId);
  }

  // Add final stages to newly created futures only; don't call this on futures found in queue as they would get these final stages added again!
  private CompletableFuture<MediaIdentification> addFinalStages(CompletableFuture<MediaIdentification> cf, ContentID contentId) {
    cf.whenComplete(this::log)
      .whenComplete((v, ex) -> WORKLOAD.complete())
      .thenRunAsync(() -> RECENT_IDENTIFICATIONS.remove(contentId), DELAYED_EXECUTOR);

    return cf;  // Purposely returning original cf here
  }

  private void log(MediaIdentification mi, Throwable t) {
    if(mi != null) {
      LOGGER.info("Enrichment [" + mi.getIdentification().getPrimaryIdentifier().getDataSource() + "] succeeded for " + mi.getStreamable() + ":\n - " + mi.getIdentification() + (mi.getDescriptor() == null ? "" : " -> " + mi.getDescriptor()));
    }
    else if(t instanceof EnrichmentException) {
      EnrichmentException e = (EnrichmentException)t;

      LOGGER.warning("Enrichment " + e.getDataSourceNames() + " failed for " + e.getStreamable() + ":\n - " + Throwables.formatAsOneLine(e));
    }
    else if(t instanceof CompletionException) {
      LOGGER.warning("Enrichment failed: " + Throwables.formatAsOneLine(t.getCause()));
    }
    else {
      LOGGER.log(Level.SEVERE, "Enrichment failed", t);
    }
  }

  private MediaIdentification enrichTask(ContentID contentId, MediaDescriptor parent) {
    try(Key key = storeConsistencyLock.lock()) {
      Streamable streamable = streamStore.findStream(contentId).orElseThrow(() -> new IllegalStateException("Stream with id " + contentId + " no longer available"));   // As tasks can take a while before they start, fetch latest state from StreamStore first
      List<String> dataSourceNames = parent == null ? streamStore.findStreamSource(contentId).getDataSourceNames() : List.of(parent.getIdentifier().getDataSource().getName());

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

  public synchronized void update(long importSourceId, List<Exceptional<List<Streamable>>> rootResults) {
    for(int rootResultIdx = 0; rootResultIdx < rootResults.size(); rootResultIdx++) {
      Exceptional<List<Streamable>> rootResult = rootResults.get(rootResultIdx);

      if(rootResult.isPresent()) {
        int scannerAndRootId = (int)importSourceId + rootResultIdx * 65536;
        Map<ContentID, Streamable> existingStreams = streamStore.findByImportSourceId(scannerAndRootId);  // Returns all active streams (not deleted)

        for(Streamable found : rootResult.get()) {
          try {
            Streamable existing = existingStreams.remove(found.getId());

            if(existing == null || !found.equals(existing)) {
              try(Key key = storeConsistencyLock.lock()) {
                streamStore.put(scannerAndRootId, found);  // Adds as new or modify it
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
    }
  }

  public Optional<CompletableFuture<MediaIdentification>> reidentifyStream(ContentID contentId) {
    try(Key key = storeConsistencyLock.lock()) {
      return streamStore.findStream(contentId).map(s -> asyncEnrich(s, NORMAL_CACHING_EXECUTOR));
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
      ContentID contentId = mediaIdentification.getStreamable().getId();

      // Store identifiers with stream:
      streamStore.putIdentification(contentId, mediaIdentification.getIdentification());

      // Store descriptors in descriptor store:
      if(mediaIdentification.getDescriptor() != null) {
        descriptorStore.add(mediaIdentification.getDescriptor());
      }
    }
  }
}

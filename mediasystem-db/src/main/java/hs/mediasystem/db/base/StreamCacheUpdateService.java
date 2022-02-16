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

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
  private static final Map<StreamID, CompletableFuture<MediaIdentification>> RECENT_IDENTIFICATIONS = new ConcurrentHashMap<>();
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");
  private static final Executor CACHED_EXECUTOR = createExecutor("StreamCacheUS-cached");
  private static final Executor DIRECT_EXECUTOR = createExecutor("StreamCacheUS-direct");
  private static final CompletableFuture<MediaIdentification> NO_PARENT = CompletableFuture.completedFuture(null);

  @Inject private LocalMediaIdentificationService identificationService;
  @Inject private DatabaseStreamStore streamStore;
  @Inject private DatabaseDescriptorStore descriptorStore;
  @Inject private DatabaseResponseCache responseCache;

  private final AutoReentrantLock storeConsistencyLock = new AutoReentrantLock();  // Used to sync actions of this class

  private final Executor forceCacheUseExecutor = createPriorityExecutor(CACHED_EXECUTOR, true, 1);  // forces cache use for any requests done
  private final Executor normalCachingExecutor = createPriorityExecutor(DIRECT_EXECUTOR, false, 1);  // normal cache use for any requests done
  private final Executor lowPriorityNormalCachingExecutor = createPriorityExecutor(DIRECT_EXECUTOR, false, 2);  // normal cache use for low priority requests
  private final Executor delayedExecutor = CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES, forceCacheUseExecutor);

  @PostConstruct
  private void postConstruct() {
    triggerInitialEnriches();
    initializePeriodicEnrichThread();
  }

  private Executor createPriorityExecutor(Executor executor, boolean forceCacheUse, int priority) {
    return r -> executor.execute(new PriorityRunnable(priority, () -> {
      responseCache.currentThreadForceCacheUse(forceCacheUse);
      r.run();
    }));
  }

  private static Executor createExecutor(String name) {
    return new ThreadPoolExecutor(5, 5, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), new NamedThreadFactory(name, Thread.NORM_PRIORITY - 2, true));
  }

  static class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {
    private static final AtomicLong ORDER = new AtomicLong();
    private static final Comparator<PriorityRunnable> COMPARATOR = Comparator.comparing(PriorityRunnable::getPriority).thenComparing(PriorityRunnable::getOrder);

    private final Runnable runnable;
    private final int priority;
    private final long order;

    public PriorityRunnable(int priority, Runnable runnable) {
      this.priority = priority;
      this.runnable = runnable;
      this.order = ORDER.incrementAndGet();
    }

    @Override
    public void run() {
      runnable.run();
    }

    private int getPriority() {
      return priority;
    }

    private long getOrder() {
      return order;
    }

    @Override
    public int compareTo(PriorityRunnable o) {
      return COMPARATOR.compare(this, o);
    }
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
        streamStore.findStreamsNeedingRefresh(500).stream().forEach(s -> asyncEnrich(s, lowPriorityNormalCachingExecutor));

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
    CompletableFuture<MediaIdentification> parentFuture = getParentFuture(streamable.getParentId().orElse(null), executor);

    activeIdentification = createTask(parentFuture, id, true, executor);

    RECENT_IDENTIFICATIONS.put(id, activeIdentification);

    return activeIdentification;
  }

  private CompletableFuture<MediaIdentification> asyncEnrich(Streamable streamable) {
    return asyncEnrich(streamable, forceCacheUseExecutor);
  }

  private CompletableFuture<MediaIdentification> getParentFuture(StreamID pid, Executor executor) {
    return pid == null ? NO_PARENT : RECENT_IDENTIFICATIONS.computeIfAbsent(pid, k -> createTask(NO_PARENT, k, false, executor));
  }

  private CompletableFuture<MediaIdentification> createTask(CompletableFuture<MediaIdentification> parentStage, StreamID streamId, boolean markEnriched, Executor executor) {
    WORKLOAD.start();

    CompletableFuture<MediaIdentification> cf = parentStage.thenApplyAsync(pmi -> enrichTask(streamId, pmi == null ? null : pmi.getDescriptor(), markEnriched), executor);

    cf.whenComplete((mi, t) -> log(mi, t, streamId))
      .whenComplete((v, ex) -> WORKLOAD.complete())
      .thenRunAsync(() -> RECENT_IDENTIFICATIONS.remove(streamId), delayedExecutor);

    return cf;  // Purposely returning original CompletableFuture here as there is no need to wait until all stages finish when this is a parent for a child
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

  private MediaIdentification enrichTask(StreamID streamId, MediaDescriptor parent, boolean markEnriched) {
    try(Key key = storeConsistencyLock.lock()) {
      Streamable streamable = streamStore.findStream(streamId).orElseThrow(() -> new IllegalStateException("Stream with id " + streamId + " no longer available"));   // As tasks can take a while before they start, fetch latest state from StreamStore first
      List<String> dataSourceNames = parent == null ? streamStore.findStreamSource(streamId).getDataSourceNames() : List.of(parent.getIdentifier().getDataSource().getName());

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
        if(markEnriched) {
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
   * @param mediaIdentification a {@link MediaIdentification} result to check for {@link Identifier}s of type COLLECTION.
   */
  private void fetchAndStoreCollectionDescriptors(MediaIdentification mediaIdentification) {
    // Production contains related Collection identifier which can be queried to get IdentifierCollection which in turn are each queried to get further Productions
    MediaDescriptor descriptor = mediaIdentification.getDescriptor();

    if(descriptor instanceof Production) {
      Production production = (Production)descriptor;

      production.getRelatedIdentifiers().stream()
        .filter(identifier -> identifier.getDataSource().getType().equals(MediaType.COLLECTION))  // After this filtering, stream consists of Collection type identifiers
        .filter(identifier -> identifier.getDataSource().getName().equals(production.getIdentifier().getDataSource().getName()))  // Only Collection type identifiers of same data source as production that contained it
        .forEach(this::fetchAndStoreCollectionItems);
    }
  }

  private void fetchAndStoreCollectionItems(Identifier collectionIdentifier) {
    try {
      IdentifierCollection descriptor = (IdentifierCollection)identificationService.query(collectionIdentifier);

      for(Identifier identifier : descriptor.getItems()) {
        try {
          descriptorStore.add(identificationService.query(identifier));
        }
        catch(Exception e) {
          LOGGER.warning("Exception while fetching descriptor for " + identifier + " in collection " + collectionIdentifier + ": " + Throwables.formatAsOneLine(e));
        }
      }

      descriptorStore.add(descriptor);
    }
    catch(Exception e) {
      LOGGER.warning("Exception while fetching collection descriptor for " + collectionIdentifier + ": " + Throwables.formatAsOneLine(e));
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

          asyncEnrich(found);  // TODO this could result in the enrich failing if the item is new, belongs to a serie and the serie data is old; next pass should fix this though (see else)
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

                    asyncEnrich(parent);
                  }
                });
              }

              // Refresh existing stream if parent has a newer enrich time:
              streamStore.findLastEnrichTime(existing.getId()).ifPresent(lastEnrichTime -> {
                if(parentLastEnrichTime.isAfter(lastEnrichTime)) {
                  LOGGER.warning("Existing stream with time " + lastEnrichTime + " has updated parent with time " + parentLastEnrichTime + " (" + parent + ") -> refetching: " + found);

                  asyncEnrich(existing, normalCachingExecutor);
                }
              });
            });
          });

          /*
           * Checks to see if items are complete; this only occurs if normal operations were interrupted.
           * Cache use should not be forced here as partial data cached may not be in sync with new data
           * that will be fetched.
           */

          identification.stream().map(Identification::getIdentifiers).flatMap(Collection::stream).forEach(identifier -> {
            if(identificationService.isQueryServiceAvailable(identifier.getDataSource())) {
              MediaDescriptor mediaDescriptor = descriptorStore.find(identifier).orElse(null);

              if(mediaDescriptor == null) {
                // One or more descriptors are missing, enrich:
                LOGGER.warning("Existing stream is missing descriptors in cache (" + identifier + ") -> refetching: " + found);

                asyncEnrich(found, normalCachingExecutor);
              }
              else if(mediaDescriptor instanceof Production) {
                Production production = (Production)mediaDescriptor;
                Identifier collectionIdentifier = production.getCollectionIdentifier().orElse(null);

                if(collectionIdentifier != null) {
                  IdentifierCollection identifierCollection = (IdentifierCollection)descriptorStore.find(collectionIdentifier).orElse(null);

                  if(identifierCollection == null) {
                    LOGGER.warning("Existing stream is missing collection data in cache (" + collectionIdentifier + ") -> refetching: " + found);

                    asyncEnrich(found, normalCachingExecutor);
                  }
                  else {
                    for(Identifier collectionItemIdentifier : identifierCollection.getItems()) {
                      if(descriptorStore.find(collectionItemIdentifier).orElse(null) == null) {
                        LOGGER.warning("Existing stream is missing collection items in cache (" + collectionItemIdentifier + " is missing, out of " + identifierCollection.getItems() + " from " + collectionIdentifier + ") -> refetching: " + found);

                        asyncEnrich(found, normalCachingExecutor);
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

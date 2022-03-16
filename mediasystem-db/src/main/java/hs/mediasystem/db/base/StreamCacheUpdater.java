package hs.mediasystem.db.base;

import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.mediamanager.MediaIdentification;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreamCacheUpdater {
  private static final Logger LOGGER = Logger.getLogger(StreamCacheUpdater.class.getName());
  private static final Map<StreamID, CompletableFuture<MediaIdentification>> RECENT_IDENTIFICATIONS = new ConcurrentHashMap<>();
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Downloading Metadata");
  private static final Executor CACHED_EXECUTOR = createExecutor("StreamCacheUS-cached");
  private static final Executor DIRECT_EXECUTOR = createExecutor("StreamCacheUS-direct");
  private static final CompletableFuture<MediaIdentification> NO_PARENT = CompletableFuture.completedFuture(null);

  @Inject private DatabaseResponseCache responseCache;

  private final Executor forceCacheUseExecutor = createPriorityExecutor(CACHED_EXECUTOR, true, 1);  // forces cache use for any requests done
  private final Executor normalCachingExecutor = createPriorityExecutor(DIRECT_EXECUTOR, false, 1);  // normal cache use for any requests done
  private final Executor lowPriorityNormalCachingExecutor = createPriorityExecutor(DIRECT_EXECUTOR, false, 2);  // normal cache use for low priority requests
  private final Executor delayedExecutor = CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES, forceCacheUseExecutor);

  enum Type {
    HIGH_CACHED, HIGH_UNCACHED, LOW_UNCACHED
  }

  private Executor enumToExecutor(Type type) {
    return switch(type) {
      case HIGH_CACHED -> forceCacheUseExecutor;
      case HIGH_UNCACHED -> normalCachingExecutor;
      case LOW_UNCACHED -> lowPriorityNormalCachingExecutor;
    };
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
  private CompletableFuture<MediaIdentification> asyncEnrich(Streamable streamable, BiFunction<StreamID, MediaIdentification, MediaIdentification> task, Executor executor) {
    StreamID id = streamable.getId();
    CompletableFuture<MediaIdentification> activeIdentification = RECENT_IDENTIFICATIONS.get(id);

    if(activeIdentification != null) {
      return activeIdentification;
    }

    // this code may modify RECENT_IDENTIFICATIONS map, so compute it first before modifying it ourselves:
    CompletableFuture<MediaIdentification> parentFuture = getParentFuture(streamable.getParentId().orElse(null), task, executor);

    activeIdentification = createTask(parentFuture, id, task, executor);

    RECENT_IDENTIFICATIONS.put(id, activeIdentification);

    return activeIdentification;
  }

  public CompletableFuture<MediaIdentification> asyncEnrich(Type type, Streamable streamable, BiFunction<StreamID, MediaIdentification, MediaIdentification> taskCreator) {
    return asyncEnrich(streamable, taskCreator, enumToExecutor(type));
  }

  private CompletableFuture<MediaIdentification> getParentFuture(StreamID pid, BiFunction<StreamID, MediaIdentification, MediaIdentification> task, Executor executor) {
    return pid == null ? NO_PARENT : RECENT_IDENTIFICATIONS.computeIfAbsent(pid, k -> createTask(NO_PARENT, k, task, executor));
  }

  private CompletableFuture<MediaIdentification> createTask(CompletableFuture<MediaIdentification> parentStage, StreamID streamId, BiFunction<StreamID, MediaIdentification, MediaIdentification> task, Executor executor) {
    WORKLOAD.start();

    CompletableFuture<MediaIdentification> cf = parentStage.thenApplyAsync(pmi -> task.apply(streamId, pmi), executor);

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
}

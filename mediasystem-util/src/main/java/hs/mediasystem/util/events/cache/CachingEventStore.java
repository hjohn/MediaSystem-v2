package hs.mediasystem.util.events.cache;

import hs.mediasystem.util.events.store.EventStore;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CachingEventStore<T> implements EventStore<T> {
  private static final Executor EXECUTOR = Executors.newCachedThreadPool();
  private static final int BLOCK_SIZE = 5000;

  private final EventStore<T> delegate;
  private final RangeCache<EventEnvelope<T>> cache = new RangeCache<>(EventEnvelope::index);
  private final Set<Long> prefetches = ConcurrentHashMap.newKeySet();

  public CachingEventStore(EventStore<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Class<T> eventType() {
    return delegate.eventType();
  }

  @Override
  public void append(Callback<T> callback, Consumer<Long> onCompletion) {
    delegate.append(callback, onCompletion);
  }

  @Override
  public EventEnvelope<T> take(long fromIndex) throws InterruptedException {
    EventEnvelope<T> cached = cache.find(fromIndex);

    prefetch(fromIndex);

    if(cached == null) {
      List<EventEnvelope<T>> results = delegate.take(fromIndex, BLOCK_SIZE);

      synchronized(cache) {
        cache.insert(fromIndex, results);

        cached = cache.find(fromIndex);
      }
    }

    return cached;
  }

  @Override
  public EventEnvelope<T> poll(long fromIndex) {
    EventEnvelope<T> cached = cache.find(fromIndex);

    prefetch(fromIndex);

    if(cached == null) {
      List<EventEnvelope<T>> results = delegate.poll(fromIndex, BLOCK_SIZE);

      if(results.isEmpty()) {
        return null;
      }

      synchronized(cache) {
        cache.insert(fromIndex, results);

        cached = cache.find(fromIndex);
      }
    }

    return cached;
  }

  private void prefetch(long fromIndex) {
    long available = cache.size(fromIndex);

    /*
     * Only prefetch if there are less entries available than half BLOCK_SIZE, and
     * the fetch would not be the same as a normal fetch (available != 0).
     */

    if(available != 0 && available < BLOCK_SIZE / 2) {
      long prefetchIndex = fromIndex + available;

      if(prefetches.add(prefetchIndex)) {
        EXECUTOR.execute(() -> {
          try {
            List<EventEnvelope<T>> results = delegate.poll(prefetchIndex, BLOCK_SIZE);

            if(!results.isEmpty()) {
              cache.insert(fromIndex, results);
            }
          }
          finally {
            prefetches.remove(prefetchIndex);
          }
        });
      }
    }
  }
}

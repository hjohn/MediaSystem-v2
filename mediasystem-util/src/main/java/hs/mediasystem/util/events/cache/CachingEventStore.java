package hs.mediasystem.util.events.cache;

import hs.mediasystem.util.events.store.EventStore;

import java.util.List;
import java.util.function.Consumer;

public class CachingEventStore<T> implements EventStore<T> {
  private final EventStore<T> delegate;
  private final RangeCache<EventEnvelope<T>> cache = new RangeCache<>(EventEnvelope::index);

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

    if(cached == null) {
      List<EventEnvelope<T>> results = delegate.take(fromIndex, 100);

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

    if(cached == null) {
      List<EventEnvelope<T>> results = delegate.poll(fromIndex, 100);

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
}

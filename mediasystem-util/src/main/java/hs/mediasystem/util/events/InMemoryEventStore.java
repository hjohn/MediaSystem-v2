package hs.mediasystem.util.events;

import hs.mediasystem.util.events.store.EventStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * An {@link EventStore} which keeps all events in memory.
 *
 * @param <T> the type of events stored by this event store
 */
public class InMemoryEventStore<T> implements EventStore<T> {
  private final List<T> events = new ArrayList<>();
  private final Class<T> eventType;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  private volatile CountDownLatch eventAvailableLatch = new CountDownLatch(1);

  /**
   * Constructs a new instance.
   *
   * @param eventType the event {@link Class} stored in this store, cannot be {@code null}
   */
  public InMemoryEventStore(Class<T> eventType) {
    this.eventType = Objects.requireNonNull(eventType, "eventType");
  }

  @Override
  public Class<T> eventType() {
    return eventType;
  }

  @Override
  public void append(Callback<T> callback, Consumer<Long> onSuccess) {
    Objects.requireNonNull(callback, "callback");
    Objects.requireNonNull(onSuccess, "onSuccess");

    writeLock.lock();

    int offset = events.size();

    try {
      callback.accept(events::add);

      eventAvailableLatch.countDown();
      eventAvailableLatch = new CountDownLatch(1);

      onSuccess.accept(events.size() - 1L);
    }
    catch(Exception e) {
      events.subList(offset, events.size()).clear();

      throw new IllegalStateException("Unable to append events", e);
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public EventEnvelope<T> take(long fromIndex) throws InterruptedException {
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
    }

    for(;;) {
      CountDownLatch latch = null;

      readLock.lock();

      try {
        if(fromIndex >= events.size()) {
          latch = this.eventAvailableLatch;
        }
      }
      finally {
        readLock.unlock();
      }

      if(latch == null) {
        break;
      }

      latch.await();  // wait for new event to become available
    }

    readLock.lock();

    try {
      return new EventEnvelope<>(fromIndex, events.get((int)fromIndex));
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public EventEnvelope<T> poll(long fromIndex) {
    if(fromIndex < 0) {
      throw new IllegalArgumentException("fromIndex must not be negative: " + fromIndex);
    }

    readLock.lock();

    try {
      return fromIndex >= events.size() ? null : new EventEnvelope<>(fromIndex, events.get((int)fromIndex));
    }
    finally {
      readLock.unlock();
    }
  }
}

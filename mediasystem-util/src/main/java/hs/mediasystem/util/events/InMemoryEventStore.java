package hs.mediasystem.util.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An {@link EventStore} which keeps all events in memory.
 *
 * @param <T> the type of events stored by this event store
 */
public class InMemoryEventStore<T> implements EventStore<T> {
  private final List<T> events = new ArrayList<>();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  private volatile CountDownLatch eventAvailableLatch = new CountDownLatch(1);

  @Override
  public long append(T event) {
    Objects.requireNonNull(event, "event");

    writeLock.lock();

    try {
      this.events.add(event);

      eventAvailableLatch.countDown();
      eventAvailableLatch = new CountDownLatch(1);

      return events.size() - 1;
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

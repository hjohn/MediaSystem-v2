package hs.mediasystem.util.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class InMemoryEventStream<T> implements EventStream<T> {
  private final List<T> events = new ArrayList<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private volatile CountDownLatch latch = new CountDownLatch(1);

  @Override
  public void push(T event) {
    push(List.of(event));
  }

  @Override
  public void push(List<T> events) {
    lock.writeLock().lock();

    try {
      this.events.addAll(events);

      latch.countDown();
      latch = new CountDownLatch(1);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Subscription subscribe(Consumer<T> consumer) {
    return subscribe(consumer, false);
  }

  @Override
  public Subscription subscribeAndWait(Consumer<T> consumer) {
    return subscribe(consumer, true);
  }

  private Subscription subscribe(Consumer<T> consumer, boolean synchronous) {
    AtomicBoolean stop = new AtomicBoolean();
    CountDownLatch tail = new CountDownLatch(1);
    Thread thread = new Thread(() -> consumer(consumer, stop, tail));

    thread.start();

    if(synchronous) {
      for(;;) {
        try {
          tail.await();
          break;
        }
        catch(InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    return () -> {
      stop.set(true);
      thread.interrupt();
    };
  }

  private void consumer(Consumer<T> consumer, AtomicBoolean stop, CountDownLatch tail) {
    int pointer = 0;
    CountDownLatch latch = this.latch;

    while(!stop.get()) {
      try {
        while(!stop.get()) {
          T event;

          lock.readLock().lockInterruptibly();

          try {
            if(pointer == events.size()) {
              break;
            }

            event = events.get(pointer++);
          }
          finally {
            latch = this.latch;
            lock.readLock().unlock();
          }

          consumer.accept(event);
        }

        tail.countDown();

        if(!stop.get()) {
          latch.await();
        }
      }
      catch(InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}

package hs.mediasystem.util.events;

import hs.mediasystem.util.events.EventStore.EventEnvelope;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SimpleEventStream<T> implements EventStream<T> {
  private final EventStore<T> eventStore;

  public SimpleEventStream(EventStore<T> eventStore) {
    this.eventStore = eventStore;
  }

  @Override
  public void push(T event) {
    push(List.of(event));
  }

  @Override
  public void push(List<T> events) {
    for(T event : events) {
      eventStore.append(event);
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
    long pointer = 0;

    if(synchronous) {
      for(;;) {
        EventEnvelope<T> event = eventStore.poll(pointer);

        if(event == null) {
          break;
        }

        pointer = event.index() + 1;

        consumer.accept(event.event());
      }
    }

    AtomicBoolean stop = new AtomicBoolean();
    long start = pointer;
    Thread thread = new Thread(() -> consumer(consumer, stop, start));

    thread.start();

    return () -> {
      stop.set(true);
      thread.interrupt();
    };
  }

  private void consumer(Consumer<T> consumer, AtomicBoolean stop, long start) {
    long pointer = start;

    while(!stop.get()) {
      try {
        EventEnvelope<T> event = eventStore.take(pointer);

        pointer = event.index() + 1;

        consumer.accept(event.event());
      }
      catch(InterruptedException e) {

        /*
         * If stop is false, restoring the interrupt would be an infinite loop because
         * the next blocking call will immediately throw again. If we only want to
         * stop the thread when unsubscribe is called, then we should ignore this
         * exception.
         */

        break;  // choose to always stop the thread, irrespective of stop flag
      }
    }

    if(!stop.get()) {
      throw new IllegalStateException("Subscription was force stopped: " + consumer);
    }
  }
}

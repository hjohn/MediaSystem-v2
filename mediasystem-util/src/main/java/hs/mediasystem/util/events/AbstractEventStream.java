package hs.mediasystem.util.events;

import hs.mediasystem.util.events.store.EventStore;
import hs.mediasystem.util.events.store.EventStore.EventEnvelope;
import hs.mediasystem.util.events.streams.Subscription;

import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractEventStream<T> {
  private static final Logger LOGGER = Logger.getLogger(AbstractEventStream.class.getName());

  private final EventStore<T> eventStore;

  public AbstractEventStream(EventStore<T> eventStore) {
    this.eventStore = eventStore;
  }

  public final void push(T event) {
    push(List.of(event));
  }

  public final void push(List<T> events) {
    if(events == null || events.isEmpty()) {
      throw new IllegalArgumentException("events cannot be null or empty: " + events);
    }

    eventStore.append(
      appender -> {
        for(T event : events) {
          appender.append(event);
        }
      },
      x -> LOGGER.info(() -> "Appended to " + eventStore + " " + events.size() + " event(s): " + events.subList(0, Math.min(5, events.size())))
    );
  }

  protected final Subscription subscribe(long offset, Consumer<? super EventEnvelope<T>> consumer) {
    AtomicBoolean stop = new AtomicBoolean();
    Phaser subscriptionBlocked = new Phaser();  // advances to next phase each time subscription becomes blocked
    AtomicLong pointer = new AtomicLong(offset);
    Thread thread = new Thread(() -> consumer(consumer, stop, subscriptionBlocked, pointer), "Stream[" + eventStore.eventType().getSimpleName() + "]:" + consumer);

    subscriptionBlocked.register();  // must be subscription is returned, as otherwise thread may not have registered yet when first join is called

    thread.start();

    return new Subscription() {

      @Override
      public synchronized void join() {
        if(stop.get()) {
          throw new IllegalStateException("Not subscribed");
        }

        // wait until subscription becomes blocked (or pass through if already blocked):
        subscriptionBlocked.register();
        subscriptionBlocked.awaitAdvance(subscriptionBlocked.arriveAndDeregister());
      }

      @Override
      public synchronized void unsubscribe() {
        stop.set(true);
        thread.interrupt();
      }
    };
  }

  private void consumer(Consumer<? super EventEnvelope<T>> consumer, AtomicBoolean stop, Phaser blocked, AtomicLong pointer) {
    while(!stop.get()) {
      try {
        EventEnvelope<T> event = eventStore.poll(pointer.get());

        if(event == null) {
          blocked.awaitAdvance(blocked.arriveAndDeregister());

          event = eventStore.take(pointer.get());

          blocked.register();
        }

        consumer.accept(event);
        pointer.set(event.index() + 1);
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
      catch(Exception e) {
        LOGGER.log(Level.SEVERE, "Subscription was stopped for: " + consumer + " due to an exception: ", e);
        break;
      }
    }

    if(!stop.get()) {
      throw new IllegalStateException("Subscription was force stopped: " + consumer);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + eventStore + "]";
  }
}

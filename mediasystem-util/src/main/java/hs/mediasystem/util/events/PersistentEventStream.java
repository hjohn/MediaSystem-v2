package hs.mediasystem.util.events;

import hs.mediasystem.util.events.store.EventStore;
import hs.mediasystem.util.events.store.EventStore.EventEnvelope;
import hs.mediasystem.util.events.streams.Sink;
import hs.mediasystem.util.events.streams.Source;

import java.util.function.Consumer;

public class PersistentEventStream<T> extends AbstractEventStream<T> implements EventSelector<T>, Sink<T> {

  public PersistentEventStream(EventStore<T> eventStore) {
    super(eventStore);
  }

  @Override
  public Source<EventEnvelope<T>> from(long offset) {
    return consumer -> subscribe(offset, consumer);
  }

  @Override
  public Source<T> plain() {
    return consumer -> subscribe(0, new Consumer<EventEnvelope<T>>() {
      @Override
      public void accept(EventEnvelope<T> event) {
        consumer.accept(event.event());
      }

      @Override
      public String toString() {
        return consumer.toString();
      }
    });
  }
}

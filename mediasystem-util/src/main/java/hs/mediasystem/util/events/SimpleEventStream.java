package hs.mediasystem.util.events;

import hs.mediasystem.util.events.store.EventStore;
import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.events.streams.Subscription;

import java.util.function.Consumer;

public class SimpleEventStream<T> extends AbstractEventStream<T> implements EventStream<T> {

  public SimpleEventStream(EventStore<T> eventStore) {
    super(eventStore);
  }

  @Override
  public Subscription subscribe(Consumer<? super T> consumer) {
    return subscribe(0, event -> consumer.accept(event.event()));
  }
}

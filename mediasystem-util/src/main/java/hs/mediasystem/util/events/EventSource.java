package hs.mediasystem.util.events;

import java.util.function.Consumer;

public interface EventSource<T> {

  Subscription subscribeEvents(Consumer<Event<T>> consumer);
  Subscription subscribeEventsAndWait(Consumer<Event<T>> consumer);

  default Subscription subscribe(Consumer<T> consumer) {
    return subscribeEvents(e -> consumer.accept(e.payload()));
  }

  default Subscription subscribeAndWait(Consumer<T> consumer) {
    return subscribeEventsAndWait(e -> consumer.accept(e.payload()));
  }
}

package hs.mediasystem.util.events;

import java.util.function.Consumer;

public interface EventSource<T> {
  Subscription subscribe(Consumer<T> consumer);
  Subscription subscribeAndWait(Consumer<T> consumer);
}

package hs.mediasystem.util.events;

import java.util.List;

public interface EventStream<T> extends EventSource<T> {
  void push(T event);
  void push(List<T> events);
}

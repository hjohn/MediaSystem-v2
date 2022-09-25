package hs.mediasystem.util.events;

import java.util.List;

/**
 * Represents a stream of events which can have new events appended to it.
 *
 * @param <T> the event type
 */
public interface EventStream<T> extends EventSource<T> {

  /**
   * Pushes the given event to the stream.
   *
   * @param event an event, cannot be {@code null}
   * @throws NullPointerException when event is {@code null}
   */
  void push(T event);

  /**
   * Pushes the given events to the stream. If any of the
   * elements in the list are {@code null}, these are silently skipped.
   *
   * @param events a list of events, cannot be {@code null}
   * @throws NullPointerException when events is {@code null}
   */
  void push(List<T> events);
}

package hs.mediasystem.util.javafx.base;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

/**
 * A target for registering and unregistering {@link EventHandler}s.
 */
public interface EventHandlerTarget {

  /**
   * Registers an event filter to this target. The filter is called when the
   * target receives an {@code Event} of the specified type during the capturing
   * phase of event delivery.
   *
   * @param <T> the specific event class of the filter
   * @param eventType the type of the events to receive by the filter
   * @param eventHandler the filter to register
   * @throws NullPointerException when the event type or filter is {@code null}
   */
  <T extends Event> void addEventFilter(EventType<T> eventType, EventHandler<? super T> eventHandler);

  /**
   * Registers an event handler to this target. The handler is called when the
   * target receives an {@code Event} of the specified type during the bubbling
   * phase of event delivery.
   *
   * @param <T> the specific event class of the handler
   * @param eventType the type of the events to receive by the handler
   * @param eventHandler the handler to register
   * @throws NullPointerException when the event type or handler is {@code null}
   */
  <T extends Event> void addEventHandler(EventType<T> eventType, EventHandler<? super T> eventHandler);

  /**
   * Unregisters a previously registered event filter from this target. One
   * filter might have been registered for different event types, so the
   * caller needs to specify the particular event type from which to
   * unregister the handler.
   *
   * @param <T> the specific event class of the filter
   * @param eventType the event type from which to unregister
   * @param eventHandler the filter to unregister
   * @throws NullPointerException if the event type or filter is {@code null}
   */
  <T extends Event> void removeEventFilter(EventType<T> eventType, EventHandler<? super T> eventHandler);

  /**
   * Unregisters a previously registered event handler from this target. One
   * handler might have been registered for different event types, so the
   * caller needs to specify the particular event type from which to
   * unregister the handler.
   *
   * @param <T> the specific event class of the handler
   * @param eventType the event type from which to unregister
   * @param eventHandler the handler to unregister
   * @throws NullPointerException if the event type or handler is {@code null}
   */
  <T extends Event> void removeEventHandler(EventType<T> eventType, EventHandler<? super T> eventHandler);
}
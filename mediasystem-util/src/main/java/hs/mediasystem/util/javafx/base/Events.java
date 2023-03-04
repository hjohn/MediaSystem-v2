package hs.mediasystem.util.javafx.base;

import com.sun.javafx.event.EventUtil;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;

public class Events {
  public static <E extends Event> void dispatchEvent(ObjectProperty<EventHandler<E>> eventHandlerProperty, E event, Event originatingEvent) {
    EventHandler<E> eventHandler = eventHandlerProperty.get();

    if(eventHandler != null) {
      eventHandler.handle(event);

      if(event.isConsumed() && originatingEvent != null) {
        originatingEvent.consume();
      }
    }
  }

  /**
   * Fires the given event to the given {@link EventTarget} and returns
   * whether it was consumed or not.
   *
   * @param target an {@link EventTarget}, cannot be {@code null}
   * @param event an {@link Event}, cannot be {@code null}
   * @return {@code true} if the event was consumed, otherwise {@code false}
   * @throws NullPointerException when either the target or event was {@code null}
   */
  public static boolean dispatchEvent(EventTarget target, Event event) {
    return EventUtil.fireEvent(Objects.requireNonNull(target, "target"), Objects.requireNonNull(event, "event")) == null;  // calls private API, assumption is it may become public soon, see ticket JDK-8303209
  }
}

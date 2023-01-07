package hs.mediasystem.util.javafx.base;

import javafx.beans.property.ObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;

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
}

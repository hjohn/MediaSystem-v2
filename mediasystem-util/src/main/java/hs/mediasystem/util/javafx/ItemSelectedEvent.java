package hs.mediasystem.util.javafx;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class ItemSelectedEvent<T> extends Event {
  private final T item;

  public ItemSelectedEvent(EventTarget eventTarget, T item) {
    super(eventTarget, eventTarget, EventType.ROOT);

    this.item = item;
  }

  public T getItem() {
    return item;
  }
}
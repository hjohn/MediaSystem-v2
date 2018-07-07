package hs.mediasystem.framework.actions;

import javafx.event.Event;

public interface ValueBuilder<T> {
  T build(Event event, T currentValue);
}

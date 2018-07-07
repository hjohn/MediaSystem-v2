package hs.mediasystem.util;

import javafx.event.Event;
import javafx.event.EventType;

public class FocusEvent extends Event {
  public static final EventType<FocusEvent> ANY = new EventType<>(EventType.ROOT, "FOCUS");
  public static final EventType<FocusEvent> FOCUS_LOST = new EventType<>(ANY, "FOCUS_LOST");
  public static final EventType<FocusEvent> FOCUS_GAINED = new EventType<>(ANY, "FOCUS_GAINED");

  public FocusEvent(boolean focused) {
    super(focused ? FOCUS_GAINED : FOCUS_LOST);
  }
}

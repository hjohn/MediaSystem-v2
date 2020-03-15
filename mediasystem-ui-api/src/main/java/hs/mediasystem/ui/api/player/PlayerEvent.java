package hs.mediasystem.ui.api.player;

import javafx.event.Event;
import javafx.event.EventType;

public class PlayerEvent extends Event {
  public enum Type {FINISHED}

  private final Type type;

  public PlayerEvent(Type type) {
    super(EventType.ROOT);
    this.type = type;
  }

  public Type getType() {
    return type;
  }
}

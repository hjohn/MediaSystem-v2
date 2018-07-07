package hs.mediasystem.plugin.library.scene;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class MediaNodeEvent extends Event {
  private final MediaNode mediaNode;

  public MediaNodeEvent(EventTarget eventTarget, MediaNode mediaNode) {
    super(eventTarget, eventTarget, EventType.ROOT);
    this.mediaNode = mediaNode;
  }

  public MediaNode getMediaNode() {
    return mediaNode;
  }
}
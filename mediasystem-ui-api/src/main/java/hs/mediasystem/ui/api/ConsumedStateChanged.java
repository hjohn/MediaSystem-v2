package hs.mediasystem.ui.api;

import hs.mediasystem.domain.stream.ContentID;

import javafx.event.Event;
import javafx.event.EventType;

public class ConsumedStateChanged extends Event {
  public static final EventType<ConsumedStateChanged> ANY = new EventType<>(EventType.ROOT, "CONSUMED_STATE");

  private final ContentID contentId;
  private final boolean consumed;

  public ConsumedStateChanged(ContentID contentId, boolean consumed) {
    super(ConsumedStateChanged.ANY);

    this.contentId = contentId;
    this.consumed = consumed;
  }

  public ContentID getContentId() {
    return contentId;
  }

  public boolean isContentConsumed() {
    return consumed;
  }
}

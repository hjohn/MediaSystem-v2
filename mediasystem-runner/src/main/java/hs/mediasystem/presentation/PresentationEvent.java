package hs.mediasystem.presentation;

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Events which act on {@link Presentation}s.<p>
 *
 * The current active presentations are captured during the event capturing phase,
 * and acted upon during the event bubbling phase.
 */
public class PresentationEvent extends Event {
  public static final EventType<PresentationEvent> ANY = new EventType<>(EventType.ROOT, "PRESENTATION");
  public static final EventType<PresentationEvent> CONTEXT_MENU = new EventType<>(ANY, "PRESENTATION_CONTEXT_MENU");
  public static final EventType<PresentationEvent> REFRESH = new EventType<>(ANY, "PRESENTATION_REFRESH");

  private final List<Presentation> presentations = new ArrayList<>();

  /**
   * Creates an event to trigger the context menu with options
   * from all active presentations.
   *
   * @return a {@link PresentationEvent}, never {@code null}
   */
  public static PresentationEvent triggerContextMenu() {
    return new PresentationEvent(CONTEXT_MENU);
  }

  /**
   * Creates an event to refresh all active presentations.
   *
   * @return a {@link PresentationEvent}, never {@code null}
   */
  public static PresentationEvent refresh() {
    return new PresentationEvent(REFRESH);
  }

  PresentationEvent(EventType<? extends PresentationEvent> type) {
    super(type);
  }

  void addPresentation(Presentation presentation) {
    this.presentations.add(presentation);
  }

  /**
   * Returns a list of all active presentations encountered by this event.
   *
   * @return a list of all active presentations encountered by this event, never {@code null} but can be empty
   */
  public List<Presentation> getPresentations() {
    return new ArrayList<>(presentations);
  }
}
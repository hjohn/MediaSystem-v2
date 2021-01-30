package hs.mediasystem.runner;

import hs.mediasystem.presentation.Presentation;

import javafx.event.Event;
import javafx.event.EventType;

public class NavigateEvent extends Event {
  public static final EventType<NavigateEvent> ANY = new EventType<>(EventType.ROOT, "NAVIGATION");

  public static final EventType<NavigateEvent> NAVIGATION_TO = new EventType<>(ANY, "NAVIGATION_TO");
//  public static final EventType<NavigateEvent> NAVIGATION_RIGHT = new EventType<>(ANY, "NAVIGATION_RIGHT");
//  public static final EventType<NavigateEvent> NAVIGATION_UP = new EventType<>(ANY, "NAVIGATION_UP");
//  public static final EventType<NavigateEvent> NAVIGATION_DOWN = new EventType<>(ANY, "NAVIGATION_DOWN");
//  public static final EventType<NavigateEvent> NAVIGATION_SELECT = new EventType<>(ANY, "NAVIGATION_SELECT");
  public static final EventType<NavigateEvent> NAVIGATION_ANCESTOR = new EventType<>(ANY, "NAVIGATION_ANCESTOR");
//
//  public static final EventType<NavigateEvent> NAVIGATION_EXIT = new EventType<>(NAVIGATION_ANCESTOR, "NAVIGATION_EXIT");
  public static final EventType<NavigateEvent> NAVIGATION_BACK = new EventType<>(NAVIGATION_ANCESTOR, "NAVIGATION_BACK");

  public static NavigateEvent to(Presentation presentation) {
    return new NavigateEvent(NAVIGATION_TO, presentation);
  }

  public static NavigateEvent back() {
    return new NavigateEvent(NAVIGATION_BACK, null);
  }

  private final Presentation presentation;

  private NavigateEvent(EventType<NavigateEvent> navigationEvent, Presentation presentation) {
    super(navigationEvent);

    this.presentation = presentation;
  }

  public Presentation getPresentation() {
    return presentation;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("NavigateEvent [");

    sb.append("source = ").append(getSource());
    sb.append(", target = ").append(getTarget());
    sb.append(", eventType = ").append(getEventType());
    sb.append(", consumed = ").append(isConsumed());
    sb.append(", to = ").append(presentation);

    return sb.toString();
  }
}
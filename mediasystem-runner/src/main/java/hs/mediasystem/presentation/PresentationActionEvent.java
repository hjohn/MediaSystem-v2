package hs.mediasystem.presentation;

import hs.mediasystem.runner.Action;

import java.util.function.Supplier;

import javafx.event.EventType;

public class PresentationActionEvent extends PresentationEvent {
  public static final EventType<PresentationActionEvent> ANY = new EventType<>(PresentationEvent.ANY, "PRESENTATION_ACTION");
  public static final EventType<PresentationActionEvent> PROPOSED = new EventType<>(ANY, "PRESENTATION_ACTION_PROPOSED");

  public static PresentationActionEvent createActionProposal(Action action) {
    return new PresentationActionEvent(PROPOSED, action);
  }

  private final Action action;

  private PresentationActionEvent(EventType<PresentationActionEvent> type, Action action) {
    super(type);

    this.action = action;
  }

  public Action getAction() {
    return action;
  }

  @Override
  public String toString() {
    return getClass().getName() + "[source=" + getSource() + "; target=" + getTarget() + "; type=" + getEventType() + "; consumed=" + isConsumed() + "; action=" + getAction() + "]";
  }
}
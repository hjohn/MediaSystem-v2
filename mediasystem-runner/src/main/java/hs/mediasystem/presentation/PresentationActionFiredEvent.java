package hs.mediasystem.presentation;

import hs.mediasystem.runner.util.action.Action;
import hs.mediasystem.runner.util.action.ActionTarget;

import javafx.event.EventType;

public class PresentationActionFiredEvent extends PresentationActionEvent {
  public static final EventType<PresentationActionFiredEvent> ANY = new EventType<>(PresentationActionEvent.ANY, "PRESENTATION_ACTION_FIRED");

  public static PresentationActionFiredEvent create(Action action, ActionTarget actionTarget, Presentation presentation) {
    return new PresentationActionFiredEvent(action, actionTarget, presentation);
  }

  private final Action action;
  private final ActionTarget actionTarget;
  private final Presentation presentation;

  private PresentationActionFiredEvent(Action action, ActionTarget actionTarget, Presentation presentation) {
    super(ANY, action);

    this.action = action;
    this.actionTarget = actionTarget;
    this.presentation = presentation;
  }

  @Override
  public Action getAction() {
    return action;
  }

  public ActionTarget getActionTarget() {
    return actionTarget;
  }

  public Presentation getPresentation() {
    return presentation;
  }

  @Override
  public String toString() {
    return getClass().getName() + "[source=" + getSource() + "; target=" + getTarget() + "; type=" + getEventType() + "; consumed=" + isConsumed() + "; action=" + getAction() + "]";
  }
}
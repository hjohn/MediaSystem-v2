package hs.mediasystem.runner;

import javafx.event.Event;
import javafx.event.EventType;

public class ExposedActionEvent extends Event {
  public static final EventType<ExposedActionEvent> ANY = new EventType<>(EventType.ROOT, "EXPOSED_ACTION");
  public static final EventType<ExposedActionEvent> ACTION_PROPOSED = new EventType<>(ANY, "EXPOSED_ACTION_PROPOSED");

  public static ExposedActionEvent createActionProposal(Action action) {
    return new ExposedActionEvent(ACTION_PROPOSED, action);
  }

  private final Action action;

  private ExposedActionEvent(EventType<ExposedActionEvent> navigationEvent, Action action) {
    super(navigationEvent);

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
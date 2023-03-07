package hs.mediasystem.runner.util.action;

import java.util.Objects;

/**
 * A combination of an {@link ActionTarget}, and a descriptor that describes
 * which action the target should perform.
 */
public class Action {
  private final ActionTarget actionTarget;
  private final String descriptor;

  /**
   * Constructs a new instance.
   *
   * @param actionTarget an {@link ActionTarget}, cannot be {@code null}
   * @param descriptor an action descriptor, cannot be {@code null}
   */
  public Action(ActionTarget actionTarget, String descriptor) {
    this.actionTarget = Objects.requireNonNull(actionTarget, "actionTarget");
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
  }

  /**
   * Returns the {@link ActionTarget}.
   *
   * @return the {@link ActionTarget}, never {@code null}
   */
  public ActionTarget getActionTarget() {
    return actionTarget;
  }

  /**
   * Returns the action descriptor.
   *
   * @return the action descriptor, never {@code null}
   */
  public String getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    return actionTarget + "#" + descriptor;
  }
}
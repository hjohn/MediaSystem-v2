package hs.mediasystem.runner.util.action;

import java.util.Objects;

/**
 * A location and descriptor for an action.
 */
public class Action {
  private final String path;
  private final String descriptor;

  /**
   * Constructs a new instance.
   *
   * @param path a path identifying the location of the action, cannot be {@code null}
   * @param descriptor a descriptor describing the action, cannot be {@code null}
   */
  public Action(String path, String descriptor) {
    this.path = Objects.requireNonNull(path, "path");
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
  }

  /**
   * Returns the action descriptor.
   *
   * @return the action descriptor, never {@code null}
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * Returns the location of the action.
   *
   * @return the location of the action, never {@code null}
   */
  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return path + "#" + descriptor;
  }
}
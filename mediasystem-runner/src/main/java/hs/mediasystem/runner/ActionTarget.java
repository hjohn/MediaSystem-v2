package hs.mediasystem.runner;

import hs.mediasystem.framework.actions.ExposedMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.beans.property.Property;
import javafx.event.Event;

/**
 * A (static) target for potential actions.
 */
public class ActionTarget {
  private static final Logger LOGGER = Logger.getLogger(ActionTarget.class.getName());

  private final ExposedMember exposedMember;
  private final String memberName;
  private final List<ExposedMember> path;

  public ActionTarget(List<ExposedMember> path) {
    if(path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be null or empty");
    }

    this.exposedMember = path.get(path.size() - 1);
    this.memberName = exposedMember.getMember().getDeclaringClass().getName() + "." + exposedMember.getName();
    this.path = Collections.unmodifiableList(new ArrayList<>(path));
  }

  public Class<?> getActionClass() {
    return path.get(0).getMember().getDeclaringClass();
//    return exposedMember.getMember().getDeclaringClass();
  }

  public ExposedMember getExposedMember() {
    return exposedMember;
  }

  public List<ExposedMember> getPath() {
    return path;
  }

  public String getMemberName() {
    return memberName;
  }

  public String getLabel(String controlType) {
    return ResourceManager.getText(exposedMember.getMember().getDeclaringClass(), exposedMember.getName() + "." + controlType + ".label");
  }

  /**
   * Triggers the given action.
   *
   * @param action the action to trigger
   * @param root the root object this actionTarget is nested under via its path
   * @param event an {@link Event}
   */
  public void doAction(String action, Object root, Event event) {
    Object parent = findDirectParentFromRoot(root);

    LOGGER.fine("Doing '" + action + "' for '" + memberName + "' of " + parent);

    exposedMember.doAction(action, parent, event);
  }

  @SuppressWarnings("unchecked")
  public <T> Property<T> getProperty(Object parent) {
    return (Property<T>)exposedMember.getMember().get(parent);
  }

  public Object findDirectParentFromRoot(Object root) {
    Object parent = root;
    Object property = null;

    for(ExposedMember pathMember : path) {
      @SuppressWarnings("unchecked")
      Object propertyValue = property == null ? root : ((Property<Object>)property).getValue();

      parent = propertyValue;

      if(pathMember.getMember().getMethod() != null) {
        break;
      }
      property = pathMember.getMember().get(propertyValue);
    }

    return parent;
  }

  @Override
  public String toString() {
    return path.get(0).getMember().getDeclaringClass().getName() + "::" + path.stream().map(ExposedMember::getName).collect(Collectors.joining("::"));
  }
}
package hs.mediasystem.runner.util.action;

import hs.mediasystem.runner.util.resource.ResourceManager;
import hs.mediasystem.util.expose.AbstractExposedNumericProperty;
import hs.mediasystem.util.expose.AbstractExposedProperty;
import hs.mediasystem.util.expose.ExposedBooleanProperty;
import hs.mediasystem.util.expose.ExposedControl;
import hs.mediasystem.util.expose.ExposedListProperty;
import hs.mediasystem.util.expose.ExposedLongProperty;
import hs.mediasystem.util.expose.ExposedMethod;
import hs.mediasystem.util.expose.ExposedNode;
import hs.mediasystem.util.expose.Trigger;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.Property;

/**
 * A (static) target for potential actions. An {@code ActionTarget} can have
 * multiple possible actions available to it.
 */
public final class ActionTarget {
  private static final Logger LOGGER = Logger.getLogger(ActionTarget.class.getName());
  private static final Pattern NUMERIC_PROPERTY_PATTERN = Pattern.compile("([a-zA-Z]+)\\s*(?:\\((.+)\\))?");

  private final ActionTarget parent;
  private final ExposedControl myControl;
  private final List<ExposedControl> path;
  private final String pathIdentifier;

  public ActionTarget(ActionTarget parent, ExposedControl exposedControl) {
    this.parent = parent;
    this.myControl = Objects.requireNonNull(exposedControl, "exposedControl");
    this.path = getPathStream().toList();
    this.pathIdentifier = path.stream().map(ExposedControl::getName).collect(Collectors.joining("."));
  }

  public String toPath() {
    return pathIdentifier;
  }

  public double getOrder() {
    return ResourceManager.getDouble(getTargetClass(), "action-target." + getTargetName() + ".order", 0);
  }

  public String getLabel() {
    return ResourceManager.getText(getTargetClass(), "action-target." + getTargetName() + ".label");
  }

  public boolean isVisible() {
    return ResourceManager.getBoolean(getTargetClass(), "action-target." + getTargetName() + ".visible", true);
  }

  /**
   * Creates a {@link Trigger} to run an action, or {@code null} if the action was unavailable.
   * If a {@link Trigger} is returned, it must be executed to complete the action.
   *
   * @param action the action to create, cannot be {@code null}
   * @param root the root object this {@code ActionTarget} is nested under via its path, cannot be {@code null}
   * @return a {@link Trigger} to complete the action, or {@code null} if the action was unavailable
   * @throws IllegalArgumentException when action does not conform to the action pattern, or when the action is unknown
   * @throws UnsupportedOperationException when an action is attempted on a target that doesn't support the action type
   */
  public Trigger<Object> createTrigger(String action, Object root) {
    Object ownerInstance = findDirectOwnerInstanceFromRoot(root);
    ExposedControl exposedControl = getExposedControl();

    if(ownerInstance == null) {
      return null;
    }

    LOGGER.fine("Doing '" + action + "' for '" + exposedControl.getName() + "' of " + ownerInstance);

    Matcher matcher = NUMERIC_PROPERTY_PATTERN.matcher(action);

    if(!matcher.matches()) {
      throw new IllegalArgumentException("Action must conform to " + NUMERIC_PROPERTY_PATTERN + ": " + action);
    }

    return switch(matcher.group(1)) {
      case "add" -> add(ownerInstance, matcher.group(2));
      case "subtract" -> subtract(ownerInstance, matcher.group(2));
      case "toggle" -> toggle(ownerInstance);
      case "next" -> next(ownerInstance);
      case "trigger" -> trigger(ownerInstance);
      default -> throw new IllegalArgumentException("Unknown action '" + action + "' for: " + this);
    };
  }

  /**
   * Returns a {@link Trigger} which can be used to run the action.  If <code>null</code>
   * the action is unavailable.
   *
   * @param <V> the type of value the trigger can return
   * @param root the object that supplies the trigger
   * @return a {@link Trigger} which can be used to run the action, or <code>null</code> if action is currently unavailable
   */
  public <V> Trigger<V> getTrigger(Object root) {
    ExposedControl exposedControl = getExposedControl();

    if(exposedControl instanceof ExposedMethod) {
      @SuppressWarnings("unchecked")
      ExposedMethod<V> exposedMethod = (ExposedMethod<V>)exposedControl;

      return exposedMethod.getTrigger(root);
    }

    return null;
  }

  public ExposedControl getExposedControl() {
    return myControl;
  }

  public Object findDirectOwnerInstanceFromRoot(Object root) {
    if(parent == null) {
      return root;
    }

    Object parentInstance = root;

    for(ExposedControl pathMember : parent.path) {
      if(!(pathMember instanceof ExposedNode)) {
        throw new IllegalStateException("Bad path to property; \"" + pathMember.getName() + "\" does not have child properties; target: " + this);
      }

      @SuppressWarnings("unchecked")
      Property<Object> property = ((AbstractExposedProperty<Object>)pathMember).getProperty(parentInstance);

      if(property == null) {
        throw new IllegalStateException("\"" + pathMember.getName() + "\" was not set; target: " + this);
      }

      parentInstance = property.getValue();
    }

    return parentInstance;
  }

  private Stream<ExposedControl> getPathStream() {
    return parent == null ? Stream.of(myControl) : Stream.concat(parent.getPathStream(), Stream.of(myControl));
  }

  private Class<?> getActionClass() {
    return parent == null ? getTargetClass() : parent.getActionClass();
  }

  private Class<?> getTargetClass() {
    Class<?> cls = myControl.getDeclaringClass();

    while(cls.getDeclaringClass() != null) {
      cls = cls.getDeclaringClass();
    }

    return cls;
  }

  private String getTargetName() {
    return myControl.getName();
  }

  private <V> Trigger<V> add(Object ownerInstance, String parameter) {
    ExposedControl exposedControl = getExposedControl();

    if(!(exposedControl instanceof AbstractExposedNumericProperty)) {
      throw new UnsupportedOperationException(exposedControl + " does not support 'add'");
    }

    @SuppressWarnings("unchecked")
    AbstractExposedNumericProperty<Number> property = (AbstractExposedNumericProperty<Number>)exposedControl;
    Number number = parseNumber(parameter);

    return Trigger.synchronous(
      e -> {
        property.add(ownerInstance, number);
        e.consume();
      }
    );
  }

  private <V> Trigger<V> subtract(Object ownerInstance, String parameter) {
    ExposedControl exposedControl = getExposedControl();

    if(!(exposedControl instanceof AbstractExposedNumericProperty)) {
      throw new UnsupportedOperationException(exposedControl + " does not support 'subtract'");
    }

    @SuppressWarnings("unchecked")
    AbstractExposedNumericProperty<Number> property = (AbstractExposedNumericProperty<Number>)exposedControl;
    Number number = parseNumber(parameter.startsWith("-") ? parameter.substring(1) : "-" + parameter);

    return Trigger.synchronous(
      e -> {
        property.add(ownerInstance, number);
        e.consume();
      }
    );
  }

  private <V> Trigger<V> toggle(Object ownerInstance) {
    ExposedControl exposedControl = getExposedControl();

    if(!(exposedControl instanceof ExposedBooleanProperty property)) {
      throw new UnsupportedOperationException(exposedControl + " does not support 'toggle'");
    }

    return Trigger.synchronous(
      e -> {
        property.toggle(ownerInstance);
        e.consume();
      }
    );
  }

  private <V> Trigger<V> trigger(Object ownerInstance) {
    ExposedControl exposedControl = getExposedControl();

    if(!(exposedControl instanceof ExposedMethod)) {
      throw new UnsupportedOperationException(exposedControl + " does not support 'trigger'");
    }

    @SuppressWarnings("unchecked")
    ExposedMethod<V> exposedMethod = (ExposedMethod<V>)exposedControl;

    return exposedMethod.getTrigger(ownerInstance);
  }

  private <V> Trigger<V> next(Object ownerInstance) {
    ExposedControl exposedControl = getExposedControl();

    if(!(exposedControl instanceof ExposedListProperty)) {
      throw new UnsupportedOperationException(exposedControl + " does not support 'next'");
    }

    @SuppressWarnings("unchecked")
    ExposedListProperty<Object> exposedProperty = (ExposedListProperty<Object>)exposedControl;
    Property<Object> property = exposedProperty.getProperty(ownerInstance);

    return Trigger.synchronous(
      e -> {
        List<Object> list = exposedProperty.getAllowedValues(ownerInstance);

        if(!list.isEmpty()) {
          int currentIndex = list.indexOf(property.getValue()) + 1;

          if(currentIndex >= list.size()) {
            currentIndex = 0;
          }

          property.setValue(list.get(currentIndex));
        }

        e.consume();
      }
    );
  }

  private Number parseNumber(String parameter) {
    if(getExposedControl() instanceof ExposedLongProperty) {  // don't rewrite as conditional, the long maybe cast to double before it is boxed
      return Long.parseLong(parameter);
    }

    return Double.parseDouble(parameter);
  }

  @Override
  public String toString() {
    return getActionClass() + "::" + pathIdentifier;
  }
}
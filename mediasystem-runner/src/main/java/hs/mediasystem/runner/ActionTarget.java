package hs.mediasystem.runner;

import hs.mediasystem.runner.util.ResourceManager;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.beans.property.Property;

/**
 * A (static) target for potential actions.
 */
public class ActionTarget {
  private static final Logger LOGGER = Logger.getLogger(ActionTarget.class.getName());
  private static final Pattern NUMERIC_PROPERTY_PATTERN = Pattern.compile("([a-zA-Z]+)\\s*(?:\\((.+)\\))?");

  private final List<ExposedControl> path;

  public ActionTarget(List<ExposedControl> path) {
    if(path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be null or empty");
    }

    this.path = List.copyOf(path);
  }

  public List<ExposedControl> getPath() {
    return path;
  }

  public Class<?> getActionClass() {
    return path.get(0).getDeclaringClass();
  }

  public Class<?> getTargetClass() {
    Class<?> cls = path.get(path.size() - 1).getDeclaringClass();

    while(cls.getDeclaringClass() != null) {
      cls = cls.getDeclaringClass();
    }

    return cls;
  }

  public String getTargetName() {
    return path.get(path.size() - 1).getName();
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

  @SuppressWarnings("unchecked")
  public <T> Property<T> getProperty(Object root) {
    Object ownerInstance = findDirectOwnerInstanceFromRoot(root);

    return ((AbstractExposedProperty<T>)getExposedControl()).getProperty(ownerInstance);
  }

  public ExposedControl getExposedControl() {
    return path.get(path.size() - 1);
  }

  public Object findDirectOwnerInstanceFromRoot(Object root) {
    Object parent = root;

    for(int i = 0; i < path.size() - 1; i++) {
      ExposedControl pathMember = path.get(i);

      if(!(pathMember instanceof ExposedNode)) {
        throw new IllegalStateException("Bad path to property; \"" + pathMember.getName() + "\" does not have child properties; target: " + this);
      }

      @SuppressWarnings("unchecked")
      Property<Object> property = ((AbstractExposedProperty<Object>)pathMember).getProperty(parent);

      if(property == null) {
        throw new IllegalStateException("\"" + pathMember.getName() + "\" was not set; target: " + this);
      }

      parent = property.getValue();
    }

    return parent;
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
    return path.get(0).getDeclaringClass().getName() + "::" + path.stream().map(ExposedControl::getName).collect(Collectors.joining("::"));
  }
}
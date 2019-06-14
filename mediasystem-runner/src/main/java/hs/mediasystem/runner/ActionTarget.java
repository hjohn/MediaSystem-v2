package hs.mediasystem.runner;

import hs.mediasystem.framework.expose.AbstractExposedProperty;
import hs.mediasystem.framework.expose.ExposedBooleanProperty;
import hs.mediasystem.framework.expose.ExposedControl;
import hs.mediasystem.framework.expose.ExposedDoubleProperty;
import hs.mediasystem.framework.expose.ExposedListProperty;
import hs.mediasystem.framework.expose.ExposedLongProperty;
import hs.mediasystem.framework.expose.ExposedMethod;
import hs.mediasystem.framework.expose.ExposedNode;
import hs.mediasystem.framework.expose.ExposedNumberProperty;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.beans.property.Property;
import javafx.concurrent.Task;
import javafx.event.Event;

/**
 * A (static) target for potential actions.
 */
public class ActionTarget {
  private static final Logger LOGGER = Logger.getLogger(ActionTarget.class.getName());
  private static final Pattern NUMERIC_PROPERTY_PATTERN = Pattern.compile("([a-zA-Z]+)\\s*(?:\\((.+)\\))?");

  private static final Map<Class<?>, Map<String, TaskHandler<?>>> TASK_HANDLERS = Map.of(
    ExposedMethod.class, Map.of(
      "trigger", (ActionEvent<ExposedMethod<Object, Object>> e) -> e.exposedProperty.call(e.parent, e.event)
    )
  );

  private static final Map<Class<?>, Map<String, VoidHandler<?, ?>>> VOID_HANDLERS = Map.of(
    ExposedLongProperty.class, Map.of(
      "add", (ActionEvent<ExposedLongProperty<Object>> e, Property<Long> p) -> p.setValue(clamp(e, p.getValue() + Long.parseLong(e.parameter))),
      "subtract", (ActionEvent<ExposedLongProperty<Object>> e, Property<Long> p) -> p.setValue(clamp(e, p.getValue() - Long.parseLong(e.parameter)))
    ),
    ExposedDoubleProperty.class, Map.of(
      "add", (ActionEvent<ExposedDoubleProperty<Object>> e, Property<Double> p) -> p.setValue(clamp(e, p.getValue() + Double.parseDouble(e.parameter))),
      "subtract", (ActionEvent<ExposedDoubleProperty<Object>> e, Property<Double> p) -> p.setValue(clamp(e, p.getValue() - Double.parseDouble(e.parameter)))
    ),
    ExposedNumberProperty.class, Map.of(
      "add", (ActionEvent<ExposedNumberProperty<Object>> e, Property<Number> p) -> p.setValue(clampNumber(e, p.getValue().doubleValue() + Double.parseDouble(e.parameter))),
      "subtract", (ActionEvent<ExposedNumberProperty<Object>> e, Property<Number> p) -> p.setValue(clampNumber(e, p.getValue().doubleValue() - Double.parseDouble(e.parameter)))
    ),
    ExposedBooleanProperty.class, Map.of(
      "toggle", (ActionEvent<ExposedBooleanProperty<Object>> e, Property<Boolean> p) -> p.setValue(!p.getValue())
    ),
    ExposedListProperty.class, Map.of(
      "next", (ActionEvent<ExposedListProperty<Object, Object>> e, Property<Object> p) -> {
        List<Object> list = e.exposedProperty.getAllowedValues(e.parent);
        int currentIndex = list.indexOf(p.getValue()) + 1;

        if(currentIndex >= list.size()) {
          currentIndex = 0;
        }

        p.setValue(list.get(currentIndex));
      }
    )
  );

  private final List<ExposedControl<?>> path;
  private final Map<String, TaskHandler<?>> taskHandlersByAction;
  private final Map<String, VoidHandler<?, ?>> voidHandlersByAction;

  public ActionTarget(List<ExposedControl<?>> path) {
    if(path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be null or empty");
    }

    this.path = Collections.unmodifiableList(new ArrayList<>(path));
    this.taskHandlersByAction = TASK_HANDLERS.get(getExposedControl().getClass());
    this.voidHandlersByAction = VOID_HANDLERS.get(getExposedControl().getClass());

    if(taskHandlersByAction == null && voidHandlersByAction == null) {
      throw new IllegalStateException("Unhandled target type: " + getExposedControl() + "; for: " + this);
    }
  }

  private static long clamp(ActionEvent<ExposedLongProperty<Object>> event, long newValue) {
    long minValue = event.exposedProperty.getMin(event.parent).getValue();
    long maxValue = event.exposedProperty.getMax(event.parent).getValue();

    if(newValue < minValue) {
      newValue = minValue;
    }
    else if(newValue > maxValue) {
      newValue = maxValue;
    }

    return newValue;
  }

  private static double clamp(ActionEvent<ExposedDoubleProperty<Object>> event, double newValue) {
    double minValue = event.exposedProperty.getMin(event.parent).getValue();
    double maxValue = event.exposedProperty.getMax(event.parent).getValue();

    if(newValue < minValue) {
      newValue = minValue;
    }
    else if(newValue > maxValue) {
      newValue = maxValue;
    }

    return newValue;
  }

  private static double clampNumber(ActionEvent<ExposedNumberProperty<Object>> event, double newValue) {
    double minValue = event.exposedProperty.getMin(event.parent).getValue().doubleValue();
    double maxValue = event.exposedProperty.getMax(event.parent).getValue().doubleValue();

    if(newValue < minValue) {
      newValue = minValue;
    }
    else if(newValue > maxValue) {
      newValue = maxValue;
    }

    return newValue;
  }

  public List<ExposedControl<?>> getPath() {
    return path;
  }

  public Class<?> getActionClass() {
    return path.get(0).getDeclaringClass();
  }

  public Class<?> getTargetClass() {
    return path.get(path.size() - 1).getDeclaringClass();
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
   * Triggers the given action.
   *
   * @param action the action to trigger
   * @param root the root object this actionTarget is nested under via its path
   * @param event an {@link Event}
   */
  public Task<Object> doAction(String action, Object root, Event event) {
    Object parent = findDirectParentFromRoot(root);
    ExposedControl<?> exposedControl = getExposedControl();

    LOGGER.fine("Doing '" + action + "' for '" + exposedControl.getName() + "' of " + parent);

    Matcher matcher = NUMERIC_PROPERTY_PATTERN.matcher(action);

    if(matcher.matches()) {
      ActionEvent<Object> actionEvent = new ActionEvent<>(parent, event, matcher.group(2), exposedControl);

      if(taskHandlersByAction != null) {
        @SuppressWarnings("unchecked")
        TaskHandler<Object> handler = (TaskHandler<Object>)taskHandlersByAction.get(matcher.group(1));

        if(handler != null) {
          return handler.handle(actionEvent);
        }
      }

      if(voidHandlersByAction != null) {
        @SuppressWarnings("unchecked")
        VoidHandler<Object, Object> handler = (VoidHandler<Object, Object>)voidHandlersByAction.get(matcher.group(1));

        if(handler != null) {
          if(exposedControl instanceof AbstractExposedProperty) {
            @SuppressWarnings("unchecked")
            AbstractExposedProperty<Object, Object> abstractExposedProperty = (AbstractExposedProperty<Object, Object>)exposedControl;

            handler.handle(actionEvent, abstractExposedProperty.getProperty(parent));
          }
          else {
            handler.handle(actionEvent, null);
          }

          return null;
        }
      }
    }

    throw new IllegalStateException("Unknown action '" + action + "' for: " + this);
  }

  @SuppressWarnings("unchecked")
  public <T> Property<T> getProperty(Object root) {
    Object parent = findDirectParentFromRoot(root);

    return ((AbstractExposedProperty<Object, T>)path.get(path.size() - 1)).getProperty(parent);
  }

  public ExposedControl<?> getExposedControl() {
    return path.get(path.size() - 1);
  }

  public Object findDirectParentFromRoot(Object root) {
    Object parent = root;

    for(int i = 0; i < path.size() - 1; i++) {
      @SuppressWarnings("unchecked")
      ExposedControl<Object> pathMember = (ExposedControl<Object>)path.get(i);

      if(!(pathMember instanceof ExposedNode)) {
        throw new IllegalStateException("Bad path to property; \"" + pathMember.getName() + "\" does not have child properties; target: " + this);
      }

      @SuppressWarnings("unchecked")
      Property<Object> property = ((AbstractExposedProperty<Object, Object>)pathMember).getProperty(parent);

      if(property == null) {
        throw new IllegalStateException("\"" + pathMember.getName() + "\" was not set; target: " + this);
      }

      parent = property.getValue();
    }

    return parent;
  }

  @Override
  public String toString() {
    return path.get(0).getDeclaringClass().getName() + "::" + path.stream().map(ExposedControl::getName).collect(Collectors.joining("::"));
  }

  private interface VoidHandler<T, U> {
    void handle(ActionEvent<T> event, Property<U> property);
  }

  private interface TaskHandler<T> {
    Task<Object> handle(ActionEvent<T> event);
  }

  class ActionEvent<T> {
    Object parent;
    Event event;
    String parameter;
    T exposedProperty;

    ActionEvent(Object parent, Event event, String parameter, T exposedProperty) {
      this.parent = parent;
      this.event = event;
      this.parameter = parameter;
      this.exposedProperty = exposedProperty;
    }
  }
}
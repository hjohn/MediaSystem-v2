package hs.mediasystem.runner;

import hs.mediasystem.framework.expose.ExposedControl;
import hs.mediasystem.framework.expose.ExposedMethod;
import hs.mediasystem.framework.expose.ExposedProperty;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

  private final List<ExposedControl<?>> path;

  public ActionTarget(List<ExposedControl<?>> path) {
    if(path == null || path.isEmpty()) {
      throw new IllegalArgumentException("path cannot be null or empty");
    }

    this.path = Collections.unmodifiableList(new ArrayList<>(path));
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
    @SuppressWarnings("unchecked")
    ExposedControl<Object> exposedControl = (ExposedControl<Object>)path.get(path.size() - 1);
    Object parent = findDirectParentFromRoot(root);

    LOGGER.fine("Doing '" + action + "' for '" + exposedControl.getName() + "' of " + parent);

    if(exposedControl instanceof ExposedMethod) {
      if(action.equals("trigger")) {
        return ((ExposedMethod<Object, Object>)exposedControl).call(parent, event);
      }

      throw new IllegalStateException("Unknown action '" + action + "' for: " + this);
    }
    else if(exposedControl instanceof ExposedProperty) {
      @SuppressWarnings("unchecked")
      ExposedProperty<Object, ?> exposedProperty = (ExposedProperty<Object, ?>)exposedControl;
      Class<?> providedType = exposedProperty.getProvidedType();

      if(providedType == null) {
        if(action.equals("next")) {
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>)exposedProperty.getAllowedValues(parent);
          @SuppressWarnings("unchecked")
          Property<Object> property = (Property<Object>)exposedProperty.getProperty(parent);
          int currentIndex = list.indexOf(property.getValue());

          currentIndex++;

          if(currentIndex >= list.size()) {
            currentIndex = 0;
          }

          property.setValue(list.get(currentIndex));
        }
        else {
          throw new IllegalStateException("Unknown action '" + action + "' for: " + this);
        }
      }
      else if(Number.class.isAssignableFrom(providedType)) {
        Matcher matcher = PATTERN.matcher(action);

        if(matcher.matches()) {
          if(matcher.group(1).equals("add")) {
            if(exposedProperty.getProvidedType().equals(Long.class)) {
              @SuppressWarnings("unchecked")
              Property<Long> property = (Property<Long>)exposedProperty.getProperty(parent);

              property.setValue(property.getValue() + Long.parseLong(matcher.group(2).trim()));
            }
            else if(exposedProperty.getProvidedType().equals(Number.class)) {
              @SuppressWarnings("unchecked")
              Property<Number> property = (Property<Number>)exposedProperty.getProperty(parent);

              property.setValue(property.getValue().doubleValue() + Double.parseDouble(matcher.group(2).trim()));
            }
            else if(exposedProperty.getProvidedType().equals(Double.class)) {
              @SuppressWarnings("unchecked")
              Property<Double> property = (Property<Double>)exposedProperty.getProperty(parent);

              property.setValue(property.getValue() + Double.parseDouble(matcher.group(2).trim()));
            }
            else {
              throw new IllegalStateException("Unknown action '" + action + "' for type: " + exposedProperty.getProvidedType() + "; for: " + this);
            }
          }
          else if(matcher.group(1).equals("subtract")) {
            if(exposedProperty.getProvidedType().equals(Long.class)) {
              @SuppressWarnings("unchecked")
              Property<Long> property = (Property<Long>)exposedProperty.getProperty(parent);

              property.setValue(property.getValue() - Long.parseLong(matcher.group(2).trim()));
            }
            else if(exposedProperty.getProvidedType().equals(Number.class)) {
              @SuppressWarnings("unchecked")
              Property<Number> property = (Property<Number>)exposedProperty.getProperty(parent);

              property.setValue(property.getValue().doubleValue() - Double.parseDouble(matcher.group(2).trim()));
            }
            else if(exposedProperty.getProvidedType().equals(Double.class)) {
              @SuppressWarnings("unchecked")
              Property<Double> property = (Property<Double>)exposedProperty.getProperty(parent);

              property.setValue(property.getValue() - Double.parseDouble(matcher.group(2).trim()));
            }
            else {
              throw new IllegalStateException("Unknown action '" + action + "' for type: " + exposedProperty.getProvidedType() + "; for: " + this);
            }
          }
          else {
            throw new IllegalStateException("Unknown action '" + action + "' for: " + this);
          }
        }
      }
      else if(Boolean.class.isAssignableFrom(providedType)) {
        if(action.equals("toggle")) {
          @SuppressWarnings("unchecked")
          Property<Boolean> property = (Property<Boolean>)exposedProperty.getProperty(parent);

          property.setValue(!property.getValue());
        }
        else {
          throw new IllegalStateException("Unknown action '" + action + "' for: " + this);
        }
      }
      else {
        throw new IllegalStateException("Unknown action '" + action + "' for: " + this);
      }
    }
    else {
      throw new IllegalStateException("Unhandled target type: " + exposedControl + "; for: " + this);
    }

    return null;
  }

  private static final Pattern PATTERN = Pattern.compile("(add|subtract)\\s*(?:\\((.+)\\))?");

  @SuppressWarnings("unchecked")
  public <T> Property<T> getProperty(Object root) {
    Object parent = findDirectParentFromRoot(root);

    return ((ExposedProperty<Object, T>)path.get(path.size() - 1)).getProperty(parent);
  }

  public ExposedControl<?> getExposedControl() {
    return path.get(path.size() - 1);
  }

  public Object findDirectParentFromRoot(Object root) {
    Object parent = root;

    for(int i = 0; i < path.size() - 1; i++) {
      @SuppressWarnings("unchecked")
      ExposedControl<Object> pathMember = (ExposedControl<Object>)path.get(i);

      if(pathMember instanceof ExposedMethod || ((ExposedProperty<?, ?>)pathMember).getProvidedType() == null) {
        throw new IllegalStateException("Bad path to property; \"" + pathMember.getName() + "\" does not have child properties; target: " + this);
      }

      @SuppressWarnings("unchecked")
      Property<Object> property = ((ExposedProperty<Object, Object>)pathMember).getProperty(parent);

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
}
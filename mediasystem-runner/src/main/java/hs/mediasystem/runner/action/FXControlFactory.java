package hs.mediasystem.runner.action;

import hs.jfx.eventstream.core.Invalidations;
import hs.mediasystem.runner.dialog.Dialogs;
import hs.mediasystem.runner.util.action.ActionTarget;
import hs.mediasystem.runner.util.resource.ResourceManager;
import hs.mediasystem.util.expose.AbstractExposedNumericProperty;
import hs.mediasystem.util.expose.AbstractExposedProperty;
import hs.mediasystem.util.expose.ExposedBooleanProperty;
import hs.mediasystem.util.expose.ExposedControl;
import hs.mediasystem.util.expose.ExposedDoubleProperty;
import hs.mediasystem.util.expose.ExposedListProperty;
import hs.mediasystem.util.expose.ExposedLongProperty;
import hs.mediasystem.util.expose.ExposedMethod;
import hs.mediasystem.util.expose.ExposedNode;
import hs.mediasystem.util.expose.Formatter;
import hs.mediasystem.util.expose.Trigger;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.Buttons;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Objects;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import javax.inject.Singleton;

/**
 * Creates controls allowing triggering of, or interaction with {@link ActionTarget}s.<p>
 */
@Singleton
public class FXControlFactory {

  /**
   * Creates a control given an action target and an instance holding the desired
   * property or method. The returned {@link Node} may not be interactive, and may
   * return only a {@link Label}. Returns {@code null} if the action was not found,
   * or is unavailable.
   *
   * @param actionTarget an {@link ActionTarget}, cannot be {@code null}
   * @param root an instance that holds the property, or method identified by the action target, cannot be {@code null}
   * @return a {@link Node}, or {@code null} if not available
   */
  public Node create(ActionTarget actionTarget, Object root) {
    Object ownerInstance = actionTarget.findDirectOwnerInstanceFromRoot(root);
    ExposedControl control = actionTarget.getExposedControl();

    if(ownerInstance == null) {
      return null;
    }

    if(control instanceof AbstractExposedProperty) {
      return createInteractiveControl(ownerInstance, control);
    }

    if(control instanceof ExposedMethod) {
      Trigger<Object> trigger = actionTarget.getTrigger(root);

      if(trigger == null) {
        return null;  // Nothing to do, don't show control
      }

      return Buttons.create(actionTarget.getLabel(), event -> trigger.run(event, task -> Dialogs.showProgressDialog(event, task)));
    }

    return new Label(actionTarget.getLabel());
  }

  /**
   * Creates only interactive controls given an action target and an instance holding the desired
   * property or method. Returns {@code null} if the action was not found, is unavailable, or
   * can't be interacted with.
   *
   * @param actionTarget an {@link ActionTarget}, cannot be {@code null}
   * @param root an instance that holds the property, or method identified by the action target, cannot be {@code null}
   * @return a {@link Node}, or {@code null} if not available
   */
  public Node createInteractiveControl(ActionTarget actionTarget, Object root) {
    Object ownerInstance = actionTarget.findDirectOwnerInstanceFromRoot(root);
    ExposedControl control = actionTarget.getExposedControl();

    if(ownerInstance == null) {
      return null;
    }

    if(control instanceof AbstractExposedProperty) {
      return createInteractiveControl(ownerInstance, control);
    }

    return null;
  }

  private static Node createInteractiveControl(Object ownerInstance, ExposedControl control) {
    if(control instanceof ExposedListProperty<?> c) {
      return createSpinner(ownerInstance, c);
    }

    if(control instanceof AbstractExposedNumericProperty<?> p) {
      if(p instanceof ExposedLongProperty lp) {
        return createSlider(
          ownerInstance,
          lp,
          Number::longValue,
          l -> (Number)l.doubleValue()
        );
      }

      if(p instanceof ExposedDoubleProperty dp) {
        return createSlider(
          ownerInstance,
          dp,
          Number::doubleValue,
          d -> d
        );
      }
    }

    if(control instanceof ExposedBooleanProperty exposedControl) {
      return createCheckBox(ownerInstance, exposedControl);
    }

    if(control instanceof ExposedNode) {
      return null;
    }

    throw new UnsupportedOperationException("Unsupported property type for interactive control: " + control);
  }

  private static Node createCheckBox(Object ownerInstance, ExposedBooleanProperty exposedControl) {
    CheckBox checkBox = new CheckBox();

    Property<Boolean> property = exposedControl.getProperty(ownerInstance);

    if(property == null) {
      return null;
    }

    if(exposedControl.isTriState()) {
      checkBox.setSelected(property.getValue() != null && property.getValue());
      checkBox.setIndeterminate(property.getValue() == null);

      InvalidationListener listener = obs -> {
        if(checkBox.isIndeterminate()) {
          property.setValue(null);
        }
        else {
          property.setValue(checkBox.isSelected());
        }
      };

      checkBox.selectedProperty().addListener(listener);
      checkBox.indeterminateProperty().addListener(listener);

      if(exposedControl.allowSelectTriState() && property.getValue() == null) {  // Only allow to go back to indeterminate if initial state was indeterminate
        checkBox.setAllowIndeterminate(true);
      }
    }
    else {
      checkBox.selectedProperty().bindBidirectional(property);
    }

    return checkBox;
  }

  private static <T> Node createSpinner(Object ownerInstance, ExposedListProperty<T> exposedControl) {
    ObservableList<T> allowedValues = exposedControl.getAllowedValues(ownerInstance);

    if(allowedValues.size() < 2) {
      return null;  // Do not show control if there are only 0 or 1 options to choose from
    }

    ListSpinnerValueFactory<T> valueFactory = new ListSpinnerValueFactory<>(allowedValues);
    Formatter<T> formatter = exposedControl.getFormatter();

    valueFactory.setWrapAround(true);
    valueFactory.setConverter(new StringConverter<>() {
      @Override
      public String toString(T v) {
        return v == null ? "(empty)"
          : formatter == null ? ResourceManager.getText(exposedControl.getDeclaringClass(), v)
          : formatter.format(v);
      }

      @Override
      public T fromString(String string) {
        throw new UnsupportedOperationException();
      }
    });

    valueFactory.valueProperty().bindBidirectional(exposedControl.getProperty(ownerInstance));

    Spinner<T> spinner = new Spinner<>(valueFactory);

    spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
    spinner.setEditable(false);

    return spinner;
  }

  private static <T extends Number> Node createSlider(Object ownerInstance, AbstractExposedNumericProperty<T> exposedProperty, Function<Number, T> fromNumber, Function<T, Number> toNumber) {
    ObservableValue<? extends Number> min = exposedProperty.getMin(ownerInstance);
    ObservableValue<? extends Number> max = exposedProperty.getMax(ownerInstance);
    double step = exposedProperty.getStep().doubleValue();

    Slider slider = new Slider();

    Property<T> value = exposedProperty.getProperty(ownerInstance);  // model

    if(value == null) {
      return null;
    }

    Invalidations.of(slider.minProperty(), slider.maxProperty())
      .subscribe(obs -> configureSlider(slider));

    /*
     * Setup min/max before binding value, as value is clamped to these values.
     * By default a slider has a range of 0 to 100, and if the min/max values are
     * not changed yet, the value may be clamped (and updated to the model)
     * unexpectedly.
     */

    slider.minProperty().bind(min);
    slider.maxProperty().bind(max);

    // Bidirectional mapping binding:
    value.when(Nodes.showing(slider)).map(toNumber).subscribe(slider.valueProperty()::setValue);
    slider.valueProperty().map(fromNumber).subscribe(value::setValue);

    slider.setBlockIncrement(step);
    slider.setShowTickMarks(true);
    slider.setShowTickLabels(true);

    Formatter<T> formatter = exposedProperty.getFormatter() == null ? Objects::toString : exposedProperty.getFormatter();

    slider.setLabelFormatter(new StringConverter<Double>() {
      @Override
      public String toString(Double value) {
        return formatter.format(fromNumber.apply(value));
      }

      @Override
      public Double fromString(String text) {
        throw new UnsupportedOperationException();
      }
    });

    HBox hbox = Containers.hbox(
      "slider-container",
      slider,
      Labels.create("slider-value", slider.valueProperty().map(v -> formatter.format(fromNumber.apply(v))))
    );

    return hbox;
  }

  private static void configureSlider(Slider slider) {
    slider.setMinorTickCount(0);
    slider.setMajorTickUnit(Double.MAX_VALUE);

    double majorTickUnit = (slider.getMax() - slider.getMin()) / 2;

    if(majorTickUnit > 0) {
      double tickCount = majorTickUnit / slider.getBlockIncrement();
      int minorTickCount = 1;

      if(Math.abs(tickCount - Math.round(tickCount)) < 0.0000001) {
        long tc = Math.round(tickCount);

        for(int i = 10; i >= 2; i--) {
          if(tc % i == 0) {
            tc /= tc / i;
            break;
          }
        }

        if(tc >= 1 && tc <= 10) {
          minorTickCount = (int)tc;
        }
      }
      else if(tickCount > 100) {
        minorTickCount = 10;
      }

      if(minorTickCount % 2 == 0 && majorTickUnit % 2 == 0) {
        majorTickUnit /= 2;
        minorTickCount /= 2;
      }

      slider.setMinorTickCount(minorTickCount - 1);
      slider.setMajorTickUnit(majorTickUnit);
    }
  }
}

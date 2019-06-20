package hs.mediasystem.runner;

import hs.mediasystem.framework.expose.AbstractExposedNumericProperty;
import hs.mediasystem.framework.expose.AbstractExposedProperty;
import hs.mediasystem.framework.expose.ExposedBooleanProperty;
import hs.mediasystem.framework.expose.ExposedControl;
import hs.mediasystem.framework.expose.ExposedDoubleProperty;
import hs.mediasystem.framework.expose.ExposedListProperty;
import hs.mediasystem.framework.expose.ExposedLongProperty;
import hs.mediasystem.framework.expose.ExposedMethod;
import hs.mediasystem.framework.expose.Formatter;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

@Singleton
public class ContextMenuHandler {
  @Inject private ActionTargetProvider actionTargetProvider;

  /*
   * In future, can add support for grouping certain options, and
   * spreading the options accross tabs.
   */

  public void handle(KeyEvent event, List<Presentation> activePresentations) {
    GridPane gridPane = new GridPane();
    int row = 0;

    for(Presentation presentation : activePresentations) {
      List<ActionTarget> actionTargets = actionTargetProvider.getActionTargets(presentation.getClass());
      List<ActionTarget> sortedAndFilteredActionTargets = actionTargets.stream()
        .filter(ActionTarget::isVisible)
        .sorted(Comparator.comparing(ActionTarget::getOrder))
        .collect(Collectors.toList());

      for(ActionTarget actionTarget : sortedAndFilteredActionTargets) {
        Node control = toControl(actionTarget, presentation);

        if(control != null) {
          String label = (String)control.getProperties().get("label");

          gridPane.addRow(row++, label == null ? new Label() : Labels.create(label, "header"), control);
        }
      }
    }

    if(row > 0) {
      gridPane.getStyleClass().add("option-menu-dialog");
      gridPane.getStylesheets().add(LessLoader.compile(getClass().getResource("option-menu-dialog.less")).toExternalForm());

      Dialogs.show(event, gridPane);

      event.consume();
    }
  }

  private static <P> Node toControl(ActionTarget actionTarget, Object root) {
    @SuppressWarnings("unchecked")
    P parent = (P)actionTarget.findDirectParentFromRoot(root);
    ExposedControl<?> control = actionTarget.getExposedControl();

    if(control instanceof AbstractExposedProperty) {
      if(control instanceof ExposedListProperty) {
        @SuppressWarnings("unchecked")
        ExposedListProperty<P, Object> exposedControl = (ExposedListProperty<P, Object>)actionTarget.getExposedControl();
        ObservableList<Object> allowedValues = exposedControl.getAllowedValues(parent);

        if(allowedValues.size() < 2) {
          return null;  // Donot show control if there are only 0 or 1 options to choose from
        }

        ListSpinnerValueFactory<Object> valueFactory = new ListSpinnerValueFactory<>(allowedValues);
        Formatter<Object> formatter = exposedControl.getFormatter();

        valueFactory.setWrapAround(true);
        valueFactory.setConverter(new StringConverter<>() {
          @Override
          public String toString(Object v) {
            return formatter == null ? ResourceManager.getText(exposedControl.getDeclaringClass(), v)
                                     : formatter.format(v);
          }

          @Override
          public Object fromString(String string) {
            throw new UnsupportedOperationException();
          }
        });

        valueFactory.valueProperty().bindBidirectional(exposedControl.getProperty(parent));

        Spinner<Object> spinner = new Spinner<>(valueFactory);

        spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        spinner.setEditable(false);
        spinner.getProperties().put("label", actionTarget.getLabel());

        return spinner;
      }

      if(control instanceof AbstractExposedNumericProperty) {
        if(control instanceof ExposedLongProperty) {
          return createSlider(
            actionTarget,
            parent,
            Number::longValue,
            l -> (Number)l.doubleValue()
          );
        }

        if(control instanceof ExposedDoubleProperty) {
          return createSlider(
            actionTarget,
            parent,
            Number::doubleValue,
            d -> d
          );
        }
      }

      if(control instanceof ExposedBooleanProperty) {
        @SuppressWarnings("unchecked")
        ExposedBooleanProperty<P> exposedControl = (ExposedBooleanProperty<P>)actionTarget.getExposedControl();

        CheckBox checkBox = new CheckBox();

        Property<Boolean> property = exposedControl.getProperty(parent);

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

        checkBox.getProperties().put("label", actionTarget.getLabel());

        return checkBox;
      }
    }

    if(control instanceof ExposedMethod) {
      Button button = new Button(actionTarget.getLabel());

      button.setOnAction(e -> {

        /*
         * ActionTarget#doAction can do two things:
         *
         * 1) Complete the action immediately on the FX thread (where this runs now); null is returned in that case
         * 2) Return a Task that must be executed asynchronously
         */

        Task<Object> task = actionTarget.doAction("trigger", root, e);

        if(task != null) {
          // Action only returned a Task, that must be executed asynchronously (otherwise it was already completed on FX thread).

          Dialogs.showProgressDialog(e, task);
        }
      });

      return button;
    }

    return new Label(actionTarget.getLabel());
  }

  private static <P, T extends Number> Node createSlider(ActionTarget actionTarget, P parent, Function<Number, T> fromNumber, Function<T, Number> toNumber) {
    @SuppressWarnings("unchecked")
    AbstractExposedNumericProperty<P, T> exposedProperty = (AbstractExposedNumericProperty<P, T>)actionTarget.getExposedControl();
    ObservableValue<? extends Number> min = exposedProperty.getMin(parent);
    ObservableValue<? extends Number> max = exposedProperty.getMax(parent);
    double step = exposedProperty.getStep().doubleValue();

    Slider slider = new Slider();

    slider.minProperty().bind(min);
    slider.maxProperty().bind(max);

    Var<Number> asNumberProperty = Var.mapBidirectional(exposedProperty.getProperty(parent), toNumber, fromNumber);

    slider.valueProperty().bindBidirectional(asNumberProperty);
    slider.getProperties().put("slider-value-reference", asNumberProperty);  // Otherwise property gets GC'd
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

    EventStreams.merge(Val.wrap(min).invalidations(), Val.wrap(max).invalidations())
      .withDefaultEvent(0)
      .subscribe(t -> configureSlider(slider));

    HBox hbox = Containers.hbox(
      slider,
      Labels.create("slider-value", Val.map(slider.valueProperty(), v -> formatter.format(fromNumber.apply(v))))
    );

    hbox.getStyleClass().add("slider-container");
    hbox.getProperties().put("label", actionTarget.getLabel());

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

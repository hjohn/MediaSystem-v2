package hs.mediasystem.runner;

import hs.jfx.eventstream.core.Invalidations;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.PresentationEvent;
import hs.mediasystem.runner.dialog.Dialogs;
import hs.mediasystem.runner.presentation.Presentations;
import hs.mediasystem.runner.util.LessLoader;
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
import hs.mediasystem.util.expose.Formatter;
import hs.mediasystem.util.expose.Trigger;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.control.Buttons;
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
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.ListSpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContextMenuHandler {
  private static final String STYLES_URL = LessLoader.compile(ContextMenuHandler.class, "option-menu-dialog.less");

  @Inject private ActionTargetProvider actionTargetProvider;

  /*
   * In future, can add support for grouping certain options, and
   * spreading the options across tabs.
   */

  public void handle(PresentationEvent event) {
    GridPane gridPane = new GridPane();
    int row = 0;

    for(Presentation presentation : event.getPresentations()) {
      List<ActionTarget> actionTargets = actionTargetProvider.getActionTargets(presentation.getClass());
      List<Node> sortedAndFilteredControls = actionTargets.stream()
        .filter(ActionTarget::isVisible)
        .sorted(Comparator.comparing(ActionTarget::getOrder))
        .map(target -> toControl(target, presentation))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

      if(!sortedAndFilteredControls.isEmpty()) {
        Class<?> cls = presentation.getClass();

        while(cls.getDeclaringClass() != null) {
          cls = cls.getDeclaringClass();
        }

        Label sectionLabel = Labels.create("section", ResourceManager.getText(cls, "section.label").toUpperCase());

        gridPane.addRow(row++, sectionLabel);

        GridPane.setColumnSpan(sectionLabel, 2);

        for(Node control : sortedAndFilteredControls) {
          String label = (String)control.getProperties().get("label");

          gridPane.addRow(row++, label == null ? new Label() : Labels.create("header", label), control);
        }
      }
    }

    if(row > 0) {
      gridPane.getStyleClass().add("option-menu-dialog");
      gridPane.getStylesheets().add(STYLES_URL);

      Presentations.showDialog(event, gridPane);

      event.consume();
    }
  }

  private static Node toControl(ActionTarget actionTarget, Object root) {
    Object ownerInstance = actionTarget.findDirectOwnerInstanceFromRoot(root);
    ExposedControl control = actionTarget.getExposedControl();

    if(ownerInstance == null) {
      return null;
    }

    if(control instanceof AbstractExposedProperty) {
      if(control instanceof ExposedListProperty) {
        @SuppressWarnings("unchecked")
        ExposedListProperty<Object> exposedControl = (ExposedListProperty<Object>)control;
        ObservableList<Object> allowedValues = exposedControl.getAllowedValues(ownerInstance);

        if(allowedValues.size() < 2) {
          return null;  // Do not show control if there are only 0 or 1 options to choose from
        }

        ListSpinnerValueFactory<Object> valueFactory = new ListSpinnerValueFactory<>(allowedValues);
        Formatter<Object> formatter = exposedControl.getFormatter();

        valueFactory.setWrapAround(true);
        valueFactory.setConverter(new StringConverter<>() {
          @Override
          public String toString(Object v) {
            return v == null ? "(empty)"
              : formatter == null ? ResourceManager.getText(exposedControl.getDeclaringClass(), v)
              : formatter.format(v);
          }

          @Override
          public Object fromString(String string) {
            throw new UnsupportedOperationException();
          }
        });

        valueFactory.valueProperty().bindBidirectional(exposedControl.getProperty(ownerInstance));

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
            ownerInstance,
            Number::longValue,
            l -> (Number)l.doubleValue()
          );
        }

        if(control instanceof ExposedDoubleProperty) {
          return createSlider(
            actionTarget,
            ownerInstance,
            Number::doubleValue,
            d -> d
          );
        }
      }

      if(control instanceof ExposedBooleanProperty exposedControl) {
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

        checkBox.getProperties().put("label", actionTarget.getLabel());

        return checkBox;
      }
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

  private static <T extends Number> Node createSlider(ActionTarget actionTarget, Object ownerInstance, Function<Number, T> fromNumber, Function<T, Number> toNumber) {
    @SuppressWarnings("unchecked")
    AbstractExposedNumericProperty<T> exposedProperty = (AbstractExposedNumericProperty<T>)actionTarget.getExposedControl();
    ObservableValue<? extends Number> min = exposedProperty.getMin(ownerInstance);
    ObservableValue<? extends Number> max = exposedProperty.getMax(ownerInstance);
    double step = exposedProperty.getStep().doubleValue();

    Slider slider = new Slider();

    Property<T> value = exposedProperty.getProperty(ownerInstance);  // model

    if(value == null) {
      return null;
    }

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

    Invalidations.of(slider.minProperty(), slider.maxProperty())
      .subscribe(obs -> configureSlider(slider));

    slider.minProperty().bind(min);
    slider.maxProperty().bind(max);

    HBox hbox = Containers.hbox(
      "slider-container",
      slider,
      Labels.create("slider-value", slider.valueProperty().map(v -> formatter.format(fromNumber.apply(v))))
    );

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

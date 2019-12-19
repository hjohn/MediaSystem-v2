package hs.mediasystem.util.javafx.control;

import java.util.Set;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;

public class Labels {
  public enum Feature {
    HIDE_IF_EMPTY;
  }

  public static Label create(String text, String styleClass, Feature... features) {
    return create(text, styleClass, null, features);
  }

  public static Label create(String text, String styleClass, BooleanExpression visibility, Feature... features) {
    Set<Feature> f = Set.of(features);
    Label label = new Label(text);

    label.getStyleClass().addAll(styleClass.split(",(?: *)"));

    BooleanExpression v = visibility;

    if(f.contains(Feature.HIDE_IF_EMPTY)) {
      if(v == null) {
        v = label.textProperty().isEmpty().not();
      }
      else {
        v.and(label.textProperty().isEmpty().not());
      }
    }

    if(v != null) {
      label.managedProperty().bind(v);
      label.visibleProperty().bind(v);
    }

    return label;
  }

  public static Label create(String styleClass, Feature... features) {
    return create("", styleClass, features);
  }

  public static Label create(String styleClass, ObservableValue<? extends String> observable) {
    return create(styleClass, observable, null);
  }

  public static Label create(String styleClass, ObservableValue<? extends String> observable, BooleanBinding visibility) {
    Label label = new Label();

    label.getStyleClass().addAll(styleClass.split(","));
    label.textProperty().bind(observable);

    if(visibility != null) {
      label.managedProperty().bind(visibility);
      label.visibleProperty().bind(visibility);
    }

    return label;
  }
}

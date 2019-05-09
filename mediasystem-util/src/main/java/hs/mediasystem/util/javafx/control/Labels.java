package hs.mediasystem.util.javafx.control;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;

public class Labels {

  public static Label create(String text, String styleClass) {
    return create(text, styleClass, null);
  }

  public static Label create(String text, String styleClass, BooleanExpression visibility) {
    Label label = new Label(text);

    label.getStyleClass().addAll(styleClass.split(",(?: *)"));

    if(visibility != null) {
      label.managedProperty().bind(visibility);
      label.visibleProperty().bind(visibility);
    }

    return label;
  }

  public static Label create(String styleClass) {
    return create("", styleClass);
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

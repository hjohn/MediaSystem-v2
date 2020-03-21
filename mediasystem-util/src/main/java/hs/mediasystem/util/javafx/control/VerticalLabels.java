package hs.mediasystem.util.javafx.control;

import java.util.function.Consumer;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.value.ObservableValue;
import javafx.geometry.VerticalDirection;

public class VerticalLabels {
  public static final Option HIDE_IF_EMPTY = label -> hideIfEmpty(label.textProperty()).accept(label);

  public static final Option hideIfEmpty(StringExpression x) {
    return hide(x.isEmpty());
  }

  public static final Option hide(BooleanExpression x) {
    return label -> {
      label.managedProperty().bind(x.not());
      label.visibleProperty().bind(x.not());
    };
  }

  public static final Option apply(Consumer<VerticalLabel> labelConsumer) {
    return label -> labelConsumer.accept(label);
  }

  public interface Option extends Consumer<VerticalLabel> {
  }

  public static VerticalLabel create(String styleClass, String text, Option... options) {
    VerticalLabel label = createLabel(styleClass, options);

    label.setText(text);

    return label;
  }

  public static VerticalLabel create(String styleClass, Option... options) {
    return create(styleClass, "", options);
  }

  public static VerticalLabel create(String styleClass, ObservableValue<? extends String> observable, Option... options) {
    VerticalLabel label = createLabel(styleClass, options);

    label.textProperty().bind(observable);

    return label;
  }

  private static VerticalLabel createLabel(String styleClass, Option... options) {
    VerticalLabel label = new VerticalLabel(VerticalDirection.DOWN);

    label.getStyleClass().addAll(styleClass.split(",(?: *)"));

    for(Option option : options) {
      option.accept(label);
    }

    return label;
  }
}

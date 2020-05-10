package hs.mediasystem.util.javafx.control;

import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

public class Labels {
  public static final Option HIDE_IF_EMPTY = label -> hideIfEmpty(label.textProperty()).accept(label);
  public static final Option REVERSE_CLIP_TEXT = label -> {
    InvalidationListener listener = (obs) -> {
      Text text = new Text();

      text.setFont(label.getFont());
      text.setText(label.getText());

      Bounds b = label.getLayoutBounds();
      Insets insets = label.getInsets();
      Region.layoutInArea(text, b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight(), 0, insets, false, false, label.getAlignment().getHpos(), label.getAlignment().getVpos(), true);

      Rectangle r = new Rectangle(-insets.getLeft(), -insets.getTop(), b.getWidth() + insets.getRight(), b.getHeight() + insets.getBottom());

      label.setClip(Shape.subtract(r, text));
    };

    label.layoutBoundsProperty().addListener(listener);
  };

  public static final Option hideIfEmpty(StringExpression x) {
    return hide(x.isEmpty());
  }

  public static final Option hide(BooleanExpression x) {
    return label -> {
      label.managedProperty().bind(x.not());
      label.visibleProperty().bind(x.not());
    };
  }

  public static final Option apply(Consumer<Label> labelConsumer) {
    return label -> labelConsumer.accept(label);
  }

  public interface Option extends Consumer<Label> {
  }

  public static Label create(String styleClasses, String text, Option... options) {
    Label label = createLabel(styleClasses, options);

    label.setText(text);

    return label;
  }

  public static Label create(String styleClasses, Option... options) {
    return create(styleClasses, "", options);
  }

  public static Label create(String styleClass, ObservableValue<? extends String> observable, Option... options) {
    Label label = createLabel(styleClass, options);

    label.textProperty().bind(observable);

    return label;
  }

  private static Label createLabel(String styleClasses, Option... options) {
    Label label = new Label();

    label.getStyleClass().addAll(styleClasses.split(",(?: *)"));

    for(Option option : options) {
      option.accept(label);
    }

    return label;
  }
}

package hs.mediasystem.util.javafx.control.csslayout;

import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class StylableStackPane extends StackPane {
  private static final StyleablePropertyFactory<StylableStackPane> FACTORY = new StyleablePropertyFactory<>(StackPane.getClassCssMetaData());

  private final StyleableProperty<String> nodes = FACTORY.createStyleableStringProperty(this, "layout", "-fx-layout", s -> s.nodes);

  private boolean resolved;

  public StylableStackPane(Node... nodes) {
    super(nodes);
  }

  public StylableStackPane() {
  }

  @Override
  protected void layoutChildren() {
    String v = nodes.getValue();

    if(v != null && !v.isEmpty() && !resolved) {
      getChildren().setAll(CssLayoutFactory.createNewChildren(v, this));
      resolved = true;
    }

    super.layoutChildren();
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return FACTORY.getCssMetaData();
  }
}

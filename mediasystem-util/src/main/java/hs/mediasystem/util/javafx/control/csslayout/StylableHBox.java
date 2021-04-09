package hs.mediasystem.util.javafx.control.csslayout;

import java.util.Arrays;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

public class StylableHBox extends HBox {
  private static final StyleablePropertyFactory<StylableHBox> FACTORY = new StyleablePropertyFactory<>(HBox.getClassCssMetaData());

  private final StyleableProperty<String> nodes = FACTORY.createStyleableStringProperty(this, "layout", "-fx-layout", s -> s.nodes);

  private boolean resolved;

  public StylableHBox(Node... nodes) {
    CssLayoutFactory.setPotentials(this, Arrays.asList(nodes));
  }

  public StylableHBox() {
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

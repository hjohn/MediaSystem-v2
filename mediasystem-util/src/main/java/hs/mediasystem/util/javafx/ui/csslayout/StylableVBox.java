package hs.mediasystem.util.javafx.ui.csslayout;

import java.util.Arrays;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class StylableVBox extends VBox implements Resolvable {
  private static final StyleablePropertyFactory<StylableVBox> FACTORY = new StyleablePropertyFactory<>(VBox.getClassCssMetaData());

  private final StyleableProperty<String> nodes = FACTORY.createStyleableStringProperty(this, "layout", "-fx-layout", s -> s.nodes);

  private boolean resolved;

  public StylableVBox(Node... nodes) {
    CssLayoutFactory.initialize(this, Arrays.asList(nodes));
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return FACTORY.getCssMetaData();
  }

  @Override
  public String getNodeLayout() {
    return nodes.getValue();
  }

  @Override
  public boolean isResolved() {
    return resolved;
  }

  @Override
  public void setResolved() {
    resolved = true;
  }
}

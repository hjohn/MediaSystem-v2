package hs.mediasystem.util.javafx.ui.csslayout;

import java.util.Arrays;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class StylableStackPane extends StackPane implements Resolvable {
  private static final StyleablePropertyFactory<StylableStackPane> FACTORY = new StyleablePropertyFactory<>(StackPane.getClassCssMetaData());
  private static final String PERCENTAGE_MARGINS = StylableStackPane.class.getName() + ":PercentageMargin";

  private final StyleableProperty<String> nodes = FACTORY.createStyleableStringProperty(this, "layout", "-fx-layout", s -> s.nodes);

  private boolean resolved;

  public StylableStackPane(Node... nodes) {
    CssLayoutFactory.initialize(this, Arrays.asList(nodes));
  }

  @Override
  protected void layoutChildren() {
    List<Node> managed = getManagedChildren();
    Insets insets = getInsets();
    double top = insets.getTop();
    double left = insets.getLeft();
    double w = getWidth() - left - insets.getRight();
    double h = getHeight() - top - insets.getBottom();

    for(Node child : managed) {
      Pos childAlignment = StackPane.getAlignment(child);

      Insets percentageMargin = (Insets)child.getProperties().get(PERCENTAGE_MARGINS);
      Insets margin = StackPane.getMargin(child);

      if(margin == null) {
        margin = Insets.EMPTY;
      }
      if(percentageMargin == null) {
        percentageMargin = Insets.EMPTY;
      }

      Insets combinedInsets = new Insets(
        h * percentageMargin.getTop() + margin.getTop(),
        w * percentageMargin.getRight() + margin.getRight(),
        h * percentageMargin.getBottom() + margin.getBottom(),
        w * percentageMargin.getLeft() + margin.getLeft()
      );

      layoutInArea(child, left, top, w, h, 0, combinedInsets, childAlignment != null ? childAlignment.getHpos() : HPos.CENTER, childAlignment != null ? childAlignment.getVpos() : VPos.CENTER);
    }
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

  public static void setPercentageMargin(Node child, Insets margin) {
    child.getProperties().put(PERCENTAGE_MARGINS, margin);
  }
}
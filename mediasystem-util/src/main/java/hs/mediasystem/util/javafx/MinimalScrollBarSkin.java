package hs.mediasystem.util.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * Scrollbar skin without increment/decrement buttons.
 */
public class MinimalScrollBarSkin implements Skin<ScrollBar> {

  private ScrollBar scrollBar;
  private Region group;

  public MinimalScrollBarSkin(final ScrollBar scrollBar) {
    this.scrollBar = scrollBar;

    this.group = new Region() {
      {
        getChildren().add(new Rectangle() {{
          getStyleClass().add("track");
          if(scrollBar.getOrientation() == Orientation.HORIZONTAL) {
            widthProperty().bind(scrollBar.widthProperty());
            setHeight(16);
          }
          else {
            setWidth(16);
            heightProperty().bind(scrollBar.heightProperty());
          }
        }});
        getChildren().add(new Rectangle() {{
          getStyleClass().add("thumb");

          NumberBinding range = Bindings.subtract(scrollBar.maxProperty(), scrollBar.minProperty());
          NumberBinding position = Bindings.divide(Bindings.subtract(scrollBar.valueProperty(), scrollBar.minProperty()), range);

          if(scrollBar.getOrientation() == Orientation.HORIZONTAL) {
            setHeight(16);
            widthProperty().bind(Bindings.max(16, scrollBar.visibleAmountProperty().divide(range).multiply(scrollBar.widthProperty())));
            xProperty().bind(Bindings.subtract(scrollBar.widthProperty(), widthProperty()).multiply(position));
          }
          else {
            setWidth(16);
            heightProperty().bind(Bindings.max(16, scrollBar.visibleAmountProperty().divide(range).multiply(scrollBar.heightProperty())));
            yProperty().bind(Bindings.subtract(scrollBar.heightProperty(), heightProperty()).multiply(position));
          }}
        });
      }

      @Override
      protected double computeMaxWidth(double height) {
        if(scrollBar.getOrientation() == Orientation.HORIZONTAL) {
          return Double.MAX_VALUE;
        }

        return 16;
      }

      @Override
      protected double computeMaxHeight(double width) {
        if(scrollBar.getOrientation() == Orientation.VERTICAL) {
          return Double.MAX_VALUE;
        }

        return 16;
      }
    };
  }

  @Override
  public void dispose() {
    scrollBar = null;
    group = null;
  }

  @Override
  public Node getNode() {
    return group;
  }

  @Override
  public ScrollBar getSkinnable() {
    return scrollBar;
  }
}

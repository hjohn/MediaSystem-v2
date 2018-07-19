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
  private Rectangle track = new Rectangle();
  private Rectangle thumb = new Rectangle();

  public MinimalScrollBarSkin(final ScrollBar scrollBar) {
    this.scrollBar = scrollBar;

    this.group = new Region() {
      NumberBinding range = Bindings.subtract(scrollBar.maxProperty(), scrollBar.minProperty());
      NumberBinding position = Bindings.divide(Bindings.subtract(scrollBar.valueProperty(), scrollBar.minProperty()), range);

      {
        // Children are added unmanaged because for some reason the height of the bar keeps changing
        // if they're managed in certain situations... not sure about the cause.
        getChildren().addAll(track, thumb);

        track.setManaged(false);
        track.getStyleClass().add("track");

        thumb.setManaged(false);
        thumb.getStyleClass().add("thumb");

        scrollBar.orientationProperty().addListener(obs -> setup());

        setup();
      }

      private void setup() {
        track.widthProperty().unbind();
        track.heightProperty().unbind();

        if(scrollBar.getOrientation() == Orientation.HORIZONTAL) {
          track.relocate(0, -16);
          track.widthProperty().bind(scrollBar.widthProperty());
          track.setHeight(16);
        }
        else {
          track.relocate(-16, 0);
          track.setWidth(16);
          track.heightProperty().bind(scrollBar.heightProperty());
        }

        thumb.xProperty().unbind();
        thumb.yProperty().unbind();
        thumb.widthProperty().unbind();
        thumb.heightProperty().unbind();

        if(scrollBar.getOrientation() == Orientation.HORIZONTAL) {
          thumb.relocate(0, -16);
          thumb.widthProperty().bind(Bindings.max(16, scrollBar.visibleAmountProperty().divide(range).multiply(scrollBar.widthProperty())));
          thumb.setHeight(16);
          thumb.xProperty().bind(Bindings.subtract(scrollBar.widthProperty(), thumb.widthProperty()).multiply(position));
        }
        else {
          thumb.relocate(-16, 0);
          thumb.setWidth(16);
          thumb.heightProperty().bind(Bindings.max(16, scrollBar.visibleAmountProperty().divide(range).multiply(scrollBar.heightProperty())));
          thumb.yProperty().bind(Bindings.subtract(scrollBar.heightProperty(), thumb.heightProperty()).multiply(position));
        }
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

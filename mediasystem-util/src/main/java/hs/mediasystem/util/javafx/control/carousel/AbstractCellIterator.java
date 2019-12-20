package hs.mediasystem.util.javafx.control.carousel;

import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;

import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.Effect;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Shape;

public abstract class AbstractCellIterator implements CellIterator {
  protected final Layout layout;
  protected final Orientation orientation;
  protected final double fractionalIndex;  // The relative index of the carousel with respect to the currently focused cell

  protected Dimension2D size;
  protected double zoom;

  private int currentIndex;

  public AbstractCellIterator(Layout layout, double fractionalIndex, Orientation orientation) {
    this.layout = layout;
    this.fractionalIndex = fractionalIndex;
    this.orientation = orientation;
  }

  protected abstract int nextIndex();

  /**
   * Returns the position of the current cell as a fraction, where each integer value represents
   * a cell position, with 0 representing the center.
   *
   * @return the position of the current cell as a fraction
   */
  protected double calculateCellIndex() {
    return fractionalIndex - currentIndex;
  }

  protected abstract PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double offset);
  protected abstract Rectangle2D calculateCellBounds();
  protected abstract Rectangle2D adjustCellRectangle(Rectangle2D cellRectangle);
  protected abstract Tuple2<Shape, Reflection> adjustTransform(PerspectiveTransform perspectiveTransform);
  protected abstract boolean hasNext();

  @Override
  public int next() {
    if(!hasNext()) {
      return -1;
    }

    return nextIndex();
  }

  @Override
  public Tuple2<PerspectiveTransform, Shape> calculate(int index, Dimension2D size, double zoom, Effect additionalEffect) {
    this.currentIndex = index;
    this.size = size;
    this.zoom = zoom;

    /*
     * Calculate the cells bounds adjusting for cell height, cell alignment and carousel
     * alignment in such a way that coordinate (0,0) is the baseline of the cell.
     */

    Rectangle2D cellRectangle = calculateCellBounds();

    if(layout.getReflectionEnabled() && orientation == Orientation.HORIZONTAL) {

      /*
       * Do additional adjustments for the reflection.
       */

      cellRectangle = adjustCellRectangle(cellRectangle);
    }

    /*
     * Create the PerspectiveTransform.
     */

    PerspectiveTransform perspectiveTransform = createPerspectiveTransform(cellRectangle, calculateCellIndex());

    /*
     * Add the reflection (if enabled) and return a clip for translucent areas (if enabled).
     */

    Reflection reflection = null;
    Shape currentClip = null;

    if(layout.getReflectionEnabled() && orientation == Orientation.HORIZONTAL) {
      Tuple2<Shape, Reflection> tuple = adjustTransform(perspectiveTransform);

      currentClip = tuple.a;
      reflection = tuple.b;
    }

    if(reflection != null) {
      perspectiveTransform.setInput(reflection);

      if(additionalEffect != null) {
        reflection.setInput(additionalEffect);
      }
    }
    else if(additionalEffect != null) {
      perspectiveTransform.setInput(additionalEffect);
    }

    return Tuple.of(perspectiveTransform, currentClip);
  }
}

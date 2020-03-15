package hs.mediasystem.util.javafx.control.carousel;

import hs.mediasystem.util.Tuple.Tuple2;

import javafx.geometry.Dimension2D;
import javafx.scene.effect.Effect;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.shape.Shape;

public interface CellIterator {

  /**
   * Returns the index of the next cell.  Indicates may not be returned in increasing
   * order, but may depend on the front-to-back order they must be displayed in.  This
   * makes it possible to clip reflections belonging to cells behind other cells so
   * partially transparent reflections donot blend with each other.
   *
   * @return the index of the next cel, or -1 if no more cells need to be displayed
   */
  int next();

  /**
   * Returns the {@link PerspectiveTransform} to apply to a cell and an optional
   * {@link Shape} to use for reflection clipping.  The clip may be null.
   *
   * @param index the fractional index of the cell relative to the current focused index
   * @param size the preferred size of the cell, cannot be null
   * @param zoom the zoom factor of the cell, 1.0 is no zoom
   * @param additionalEffect an optional additional {@link Effect} to apply to the cell, can be null
   * @return a tuple of {@link PerspectiveTransform} and {@link Shape}, never null
   */
  Tuple2<PerspectiveTransform, Shape> calculate(int index, Dimension2D size, double zoom, Effect additionalEffect);
}

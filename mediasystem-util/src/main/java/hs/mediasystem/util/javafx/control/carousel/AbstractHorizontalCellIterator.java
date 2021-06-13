package hs.mediasystem.util.javafx.control.carousel;

import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;

import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public abstract class AbstractHorizontalCellIterator extends AbstractCellIterator {
  private final boolean vertical;

  public AbstractHorizontalCellIterator(Layout layout, double fractionalIndex, Orientation orientation) {
    super(layout, fractionalIndex, orientation);

    this.vertical = orientation == Orientation.VERTICAL;
  }

  /**
   * Returns the width and height of the cell when it is made to fit within
   * the MaxCellWidth and MaxCellHeight restrictions while preserving the aspect
   * ratio.
   *
   * @return the normalized dimensions
   */
  protected Dimension2D getNormalizedCellSize() {
    double prefWidth = size.getWidth();
    double prefHeight = size.getHeight();

    if(prefWidth > layout.getMaxCellWidth()) {
      prefHeight = prefHeight / prefWidth * layout.getMaxCellWidth();
      prefWidth = layout.getMaxCellWidth();
    }
    if(prefHeight > layout.getMaxCellHeight()) {
      prefWidth = prefWidth / prefHeight * layout.getMaxCellHeight();
      prefHeight = layout.getMaxCellHeight();
    }

    return new Dimension2D(prefWidth, prefHeight);
  }

  @Override
  protected Rectangle2D calculateCellBounds() {
    Dimension2D cellSize = getNormalizedCellSize();

    double cellWidth = cellSize.getWidth();
    double cellHeight = cellSize.getHeight();

    double cellX = vertical
      ? -layout.getMaxCellWidth() * layout.getViewAlignment() + (layout.getMaxCellWidth() - cellWidth) * layout.getCellAlignment()
      : -0.5 * cellWidth;

    double cellY = vertical
      ? -0.5 * cellHeight
      : -layout.getMaxCellHeight() * layout.getViewAlignment() + (layout.getMaxCellHeight() - cellHeight) * layout.getCellAlignment();

    return new Rectangle2D(cellX, cellY, cellWidth, cellHeight);
  }

  @Override
  protected Rectangle2D adjustCellRectangleForReflection(Rectangle2D cellRectangle) {
    double reflectionMaxHeight = layout.getReflectionMaxHeight();

    double height = cellRectangle.getHeight();
    double unusedHeight = layout.getMaxCellHeight() - height;

    double horizonDistance = unusedHeight - unusedHeight * layout.getCellAlignment() + layout.getReflectionHorizonDistance();
    double reflectionPortion = (reflectionMaxHeight - horizonDistance) / height;

    if(reflectionPortion < 0 || horizonDistance >= reflectionMaxHeight) {
      return cellRectangle;
    }

    if(reflectionPortion > 1) {
      reflectionPortion = 1;
    }

    return new Rectangle2D(
      cellRectangle.getMinX(),
      cellRectangle.getMinY(),
      cellRectangle.getWidth(),
      cellRectangle.getHeight() + 2 * horizonDistance + height * reflectionPortion
    );
  }

  private static final double REFLECTION_OPACITY = 0.5;

  @Override
  protected Tuple2<Shape, Reflection> adjustTransformForReflection(PerspectiveTransform perspectiveTransform) {
    double reflectionMaxHeight = layout.getReflectionMaxHeight();

    double cellHeight = getNormalizedCellSize().getHeight();
    double unusedHeight = layout.getMaxCellHeight() - cellHeight;

    double horizonDistance = unusedHeight - unusedHeight * layout.getCellAlignment() + layout.getReflectionHorizonDistance();
    double reflectionPortion = (reflectionMaxHeight - horizonDistance) / cellHeight;

    if(reflectionPortion < 0 || horizonDistance >= reflectionMaxHeight) {
      return Tuple.of(null, null);
    }

    double reflectionTopOpacity = REFLECTION_OPACITY - REFLECTION_OPACITY / reflectionMaxHeight * horizonDistance;
    double reflectionBottomOpacity = 0;

    if(reflectionPortion > 1) {
      reflectionBottomOpacity = REFLECTION_OPACITY - REFLECTION_OPACITY / reflectionPortion;
      reflectionPortion = 1;
    }

    Shape shape = null;
    Reflection reflection = null;

    if(reflectionPortion > 0) {
      reflection = new Reflection(2 * horizonDistance / cellHeight * size.getHeight(), reflectionPortion, reflectionTopOpacity, reflectionBottomOpacity);

      if(layout.getClipReflections()) {
        double reflectionY = cellHeight + 2 * horizonDistance;
        double fullHeight = reflectionY + cellHeight * reflectionPortion;

        double reflectionLY = perspectiveTransform.getUly() + (perspectiveTransform.getLly() - perspectiveTransform.getUly()) / fullHeight * reflectionY;
        double reflectionRY = perspectiveTransform.getUry() + (perspectiveTransform.getLry() - perspectiveTransform.getUry()) / fullHeight * reflectionY;

        shape = new Polygon(
          perspectiveTransform.getUlx(), reflectionLY,
          perspectiveTransform.getUrx(), reflectionRY,
          perspectiveTransform.getLrx(), perspectiveTransform.getLry(),
          perspectiveTransform.getLlx(), perspectiveTransform.getLly()
        );
      }
    }

    return Tuple.of(shape, reflection);
  }
}

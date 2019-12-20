package hs.mediasystem.util.javafx.control.carousel;

import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.PerspectiveTransform;

public class LinearCellIterator extends AbstractHorizontalCellIterator {
  private final LinearLayout layout;
  private final int baseIndex;
  private final double halfSize;
  private final boolean vertical;
  private final Dimension2D size;
  private final int maxItems;

  private double minSize = Double.MAX_VALUE;
  private double maxSize = Double.MIN_VALUE;
  private int nextCount;
  private int previousCount;

  public LinearCellIterator(LinearLayout layout, double fractionalIndex, Orientation orientation, Dimension2D size, int maxItems) {
    super(layout, fractionalIndex, orientation);

    this.layout = layout;
    this.size = size;
    this.maxItems = maxItems;
    this.vertical = orientation == Orientation.VERTICAL;

    int centerIndex = (int)Math.round(fractionalIndex);

    this.baseIndex = centerIndex == -1 ? 0 : centerIndex;
    this.halfSize = (vertical ? size.getHeight() : size.getWidth()) / 2;
  }

  private boolean hasMorePreviousCells() {
    return minSize > -halfSize && baseIndex - previousCount - 1 >= 0;
  }

  private boolean hasMoreNextCells() {
    return maxSize < halfSize && baseIndex + nextCount <= maxItems - 1;
  }

  @Override
  public boolean hasNext() {
    return hasMorePreviousCells() || hasMoreNextCells();
  }

  @Override
  protected int nextIndex() {
    if((hasMorePreviousCells() && previousCount < nextCount) || !hasMoreNextCells()) {
      return baseIndex - previousCount++ - 1;
    }

    return baseIndex + nextCount++;
  }

  @Override
  protected PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double index) {
    double offset = index / layout.getDensity();

    offset += (vertical ? size.getHeight() : size.getWidth()) * (0.5 - layout.getCenterPosition());

    double maxCellSize = vertical ? layout.getMaxCellWidth() : layout.getMaxCellHeight();
    double horizon = 0.5 * maxCellSize - maxCellSize * layout.getViewAlignment();

    double offsetX = vertical ? horizon : offset;
    double offsetY = vertical ? offset : horizon;

    PerspectiveTransform perspectiveTransform = new PerspectiveTransform(
      cellRectangle.getMinX() * zoom - offsetX, cellRectangle.getMinY() * zoom - offsetY,
      cellRectangle.getMaxX() * zoom - offsetX, cellRectangle.getMinY() * zoom - offsetY,
      cellRectangle.getMaxX() * zoom - offsetX, cellRectangle.getMaxY() * zoom - offsetY,
      cellRectangle.getMinX() * zoom - offsetX, cellRectangle.getMaxY() * zoom - offsetY
    );

    minSize = Math.min(minSize, vertical ? perspectiveTransform.getUly() : perspectiveTransform.getUlx());
    maxSize = Math.max(maxSize, vertical ? perspectiveTransform.getLly() : perspectiveTransform.getUrx());

    return perspectiveTransform;
  }
}

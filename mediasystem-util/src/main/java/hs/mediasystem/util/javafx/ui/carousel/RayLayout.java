package hs.mediasystem.util.javafx.ui.carousel;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;

public class RayLayout extends Layout {

  /**
   * The radius of the carousel circle expressed as a fraction of half the view's width.
   * This controls how wide the carousel appears in view size independent fashion.<p>
   *
   * Note that a 3D representation of the carousel can appear less wide (or wider)
   * than the property indicates; this is because cells placed further away from the viewer
   * will appear smaller.  Only if all cells are placed near the viewer (which can be
   * achieved by setting {@link #viewDistanceRatioProperty()} to a high value) will the cells
   * appear to use the fraction of the view as expressed by this property.<p>
   *
   * Defaults to 1.0 which would cover the whole view horizontally if the view distance ratio
   * is high.
   *
   * @return the radius ratio property
   */
  public final DoubleProperty radiusRatioProperty() { return radiusRatio; }
  public final double getRadiusRatio() { return radiusRatio.get(); }
  private final DoubleProperty radiusRatio = new SimpleDoubleProperty(1.0);

  /**
   * The distance of the camera (from the carousel's center) expressed as a fraction
   * of the carousel's radius.  Higher values will result in less perspective.  Note
   * that this value is independent of how large the carousel appears, it only changes
   * the perspective.<p>
   *
   * Setting this very high or low (<-100 or >100) will make the carousel appear like a
   * ribbon without any perspective.  Setting this near 1.0 will put the camera near the
   * edge of the carousel, resulting in badly distorted cells.  Setting this below 1.0
   * will result in the cells being viewed from the opposite side and their perspective
   * will therefore be inverted (a carousel would curve towards the viewer, instead of
   * away).
   *
   * Defaults to 2.0.
   *
   * @return the view distance ratio property
   */
  public final DoubleProperty viewDistanceRatioProperty() { return viewDistanceRatio; }
  public final double getViewDistanceRatio() { return viewDistanceRatio.get(); }
  private final DoubleProperty viewDistanceRatio = new SimpleDoubleProperty(2.0);

  /**
   * The fraction of the carousel's circle that is used to display cells.  1.0 would
   * use the entire circle to display cells and 0.5 half of it.<p>
   *
   * Note that this does not change the amount of cells displayed, only how they are
   * spread along the carousel edge.<p>
   *
   * Defaults to 0.5.
   *
   * @return the carousel view fraction property
   */
  public final DoubleProperty carouselViewFractionProperty() { return carouselViewFraction; }
  public final double getCarouselViewFraction() { return carouselViewFraction.get(); }
  private final DoubleProperty carouselViewFraction = new SimpleDoubleProperty(0.5);

  public RayLayout() {
    radiusRatioProperty().addListener(this::triggerInvalidation);
    viewDistanceRatioProperty().addListener(this::triggerInvalidation);
    carouselViewFractionProperty().addListener(this::triggerInvalidation);
  }

  @Override
  public CellIterator renderCellIterator(double fractionalIndex, Orientation orientation, Dimension2D size, int maxItems) {
    return new RayCellIterator(this, fractionalIndex, orientation, size, maxItems);
  }

  /**
   * Called by the CellIterator for each cell to allow broad customization of the
   * current cell.  The customization options are CellIterator specific.  To customize
   * a cell, examine the CellIterator's state and apply adjustments to the cell itself
   * or any of the intermediate values being calculated (if provided).<p>
   *
   * @param iterator a CellIterator
   */
  protected void customizeCell(RayCellIterator iterator) {
    double index = iterator.calculateCellIndex();

    rotateCenterCellsTowardsViewer(iterator, 2.0, index);
  }

  public static void fadeOutEdgeCells(RayCellIterator iterator, CarouselListCell<?> cell, double fadeOutCellCount, double index) {
    double fadeOutDistance = iterator.getCellCount() / 2 - fadeOutCellCount + 0.5;

    if(Math.abs(index) > fadeOutDistance) {
      cell.setOpacity(1.0 - (Math.abs(index) - fadeOutDistance) / fadeOutCellCount);
    }
    else {
      cell.setOpacity(1.0);
    }
  }

  @Override
  protected void postProcessCell(CellIterator iterator, CarouselListCell<?> cell, double fractionalIndex) {
    fadeOutEdgeCells((RayCellIterator)iterator, cell, 0.5, fractionalIndex - cell.getIndex());
  }

  protected void rotateCenterCellsTowardsViewer(RayCellIterator iterator, double cellsToRotate, double index) {
    Point3D[] points = iterator.currentPoints();

    if(index < cellsToRotate) {
      double angle = index > -cellsToRotate ? 0.5 * Math.PI * -index / cellsToRotate + 0.5 * Math.PI : Math.PI;

      Point3D center = new Point3D((points[0].getX() + points[1].getX()) * 0.5, 0, (points[0].getZ() + points[1].getZ()) * 0.5);

      for(int i = 0; i < points.length; i++) {
        points[i] = rotateY(points[i], center, angle);
      }
    }
  }

  protected static Point3D rotateY(Point3D p, Point3D center, double radians) {
    Point3D input = new Point3D(p.getX() - center.getX(), p.getY() - center.getY(), p.getZ() - center.getZ());

    return new Point3D(
      input.getZ() * Math.sin(radians) + input.getX() * Math.cos(radians) + center.getX(),
      input.getY() + center.getY(),
      input.getZ() * Math.cos(radians) - input.getX() * Math.sin(radians) + center.getZ()
    );
  }
}

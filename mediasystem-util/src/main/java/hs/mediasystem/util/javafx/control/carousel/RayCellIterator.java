package hs.mediasystem.util.javafx.control.carousel;

import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.PerspectiveTransform;

public class RayCellIterator extends AbstractHorizontalCellIterator {
  private final RayLayout layout;
  private final int baseIndex;
  private final int minimumIndex;
  private final int maximumIndex;
  private final int cellCount;
  private final Dimension2D size;

  private Point3D[] points;

  private int nextCount;
  private int previousCount;

  public RayCellIterator(RayLayout layout, double fractionalIndex, Orientation orientation, Dimension2D size, int maxItems) {
    super(layout, fractionalIndex, orientation);

    this.layout = layout;
    this.size = size;

    int centerIndex = (int)Math.round(fractionalIndex);

    this.baseIndex = centerIndex == -1 ? 0 : centerIndex;

    int count = (int)calculateCellCount();

    this.cellCount = count % 2 == 0 ? count - 1 : count;  // always uneven
    this.minimumIndex = Math.max(0, centerIndex - cellCount / 2);
    this.maximumIndex = Math.min(maxItems - 1, centerIndex + cellCount / 2);
  }

  public int getCellCount() {
    return cellCount;
  }

  public Point3D[] currentPoints() {
    return points;
  }

  @Override
  protected PerspectiveTransform createPerspectiveTransform(Rectangle2D cellRectangle, double index) {

    /*
     * Calculate where the cell bounds are in 3D space based on its index position on the
     * carousel.
     */

    this.points = calculateCarouselCoordinates(cellRectangle, index);

    /*
     * Apply additional transformations to the cell's 3D coordinates based on its index.
     */

    layout.customizeCell(this);

    /*
     * Project the final position to 2D space.
     */

    Point2D[] projectedPoints = project(points);

    /*
     * Return the PerspectiveTransform
     */

    double cx = (projectedPoints[0].getX() + projectedPoints[1].getX() + projectedPoints[2].getX() + projectedPoints[3].getX()) / 4;
    double cy = (projectedPoints[0].getY() + projectedPoints[1].getY() + projectedPoints[2].getY() + projectedPoints[3].getY()) / 4;

    return new PerspectiveTransform(
      (projectedPoints[0].getX() - cx) * zoom + projectedPoints[0].getX(), (projectedPoints[0].getY() - cy) * zoom + projectedPoints[0].getY(),
      (projectedPoints[1].getX() - cx) * zoom + projectedPoints[1].getX(), (projectedPoints[1].getY() - cy) * zoom + projectedPoints[1].getY(),
      (projectedPoints[2].getX() - cx) * zoom + projectedPoints[2].getX(), (projectedPoints[2].getY() - cy) * zoom + projectedPoints[2].getY(),
      (projectedPoints[3].getX() - cx) * zoom + projectedPoints[3].getX(), (projectedPoints[3].getY() - cy) * zoom + projectedPoints[3].getY()
    );
  }

  protected Point2D[] project(Point3D[] points) {
    double carouselRadius = getCarouselRadius();
    double viewDistance = layout.getViewDistanceRatio() * carouselRadius;
    double fov = viewDistance - carouselRadius;
    double horizonY = layout.getMaxCellHeight() * layout.getViewAlignment() - 0.5 * layout.getMaxCellHeight();

    Point2D[] projectedPoints = new Point2D[points.length];
    double centerShift = -getCarouselRadius() * (0.5 - layout.getCenterPosition()) / layout.getRadiusRatio();

    for(int i = 0; i < points.length; i++) {
      projectedPoints[i] = project(points[i], viewDistance, fov, horizonY, centerShift);
    }

    return projectedPoints;
  }

  private static Point2D project(Point3D p, double viewDistance, double fov, double horizonY, double centerShift) {
    return new Point2D(p.getX() * fov / (p.getZ() + viewDistance) + centerShift, p.getY() * fov / (p.getZ() + viewDistance) + horizonY);
  }

  protected Point3D[] calculateCarouselCoordinates(Rectangle2D cellRectangle, double index) {
    double angleOnCarousel = 2 * Math.PI * layout.getCarouselViewFraction() / calculateCellCount() * index + 0.5 * Math.PI;

    double cos = Math.cos(angleOnCarousel);
    double sin = -Math.sin(angleOnCarousel);

    double centerShift = 0;//-getCarouselRadius() * (layout.getCenterPosition() - 0.5) / layout.getRadiusRatio();
    double l = getCarouselRadius() * zoom - cellRectangle.getMinX();
    double r = l - cellRectangle.getWidth();

    double lx = l * cos + centerShift;
    double rx = r * cos + centerShift;
    double ty = cellRectangle.getMinY();
    double by = ty + cellRectangle.getHeight();
    double lz = l * sin;
    double rz = r * sin;

    return new Point3D[] {new Point3D(lx, ty, lz), new Point3D(rx, ty, rz), new Point3D(rx, by, rz), new Point3D(lx, by, lz)};
  }

  protected double getCarouselRadius() {
    return size.getWidth() * layout.getRadiusRatio();
  }

  protected double calculateCellCount() {
    double count = size.getWidth() * layout.getDensity();

    return count < 3 ? 3 : count;
  }

  private boolean hasMoreLeftCells() {
    return baseIndex - previousCount - 1 >= minimumIndex;
  }

  private boolean hasMoreRightCells() {
    return baseIndex + nextCount <= maximumIndex;
  }

  @Override
  public boolean hasNext() {
    return hasMoreLeftCells() || hasMoreRightCells();
  }

  @Override
  protected int nextIndex() {
    if((hasMoreLeftCells() && previousCount < nextCount) || !hasMoreRightCells()) {
      return baseIndex - previousCount++ - 1;
    }

    return baseIndex + nextCount++;
  }
}

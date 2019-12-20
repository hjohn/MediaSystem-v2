package hs.mediasystem.util.javafx.control.carousel;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;

public abstract class Layout {

  /**
   * The vertical alignment of cells which donot utilize all of the maximum available height.
   */
  public final DoubleProperty cellAlignmentProperty() { return cellAlignment; }
  public final double getCellAlignment() { return cellAlignment.get(); }
  private final DoubleProperty cellAlignment = new SimpleDoubleProperty(0.8);

  public final BooleanProperty reflectionEnabledProperty() { return reflectionEnabled; }
  public final boolean getReflectionEnabled() { return reflectionEnabled.get(); }
  private final BooleanProperty reflectionEnabled = new SimpleBooleanProperty(true);

  public final BooleanProperty clipReflectionsProperty() { return clipReflections; }
  public final boolean getClipReflections() { return clipReflections.get(); }
  private final BooleanProperty clipReflections = new SimpleBooleanProperty(true);

  public final DoubleProperty reflectionMaxHeight() { return reflectionMaxHeight; }
  public double getReflectionMaxHeight() { return reflectionMaxHeight.get(); }
  private final DoubleProperty reflectionMaxHeight = new SimpleDoubleProperty(50);

  public final DoubleProperty reflectionHorizonDistance() { return reflectionHorizonDistance; }
  public double getReflectionHorizonDistance() { return reflectionHorizonDistance.get(); }
  private final DoubleProperty reflectionHorizonDistance = new SimpleDoubleProperty(5);

  /**
   * The number of cells to fit in a single pixel.
   */
  public final DoubleProperty densityProperty() { return density; }
  public final double getDensity() { return density.get(); }
  private final DoubleProperty density = new SimpleDoubleProperty(0.02);

  /**
   * The maximum width a cell is allowed to become.
   */
  public final DoubleProperty maxCellWidthProperty() { return maxCellWidth; }
  public final double getMaxCellWidth() { return maxCellWidth.get(); }
  private final DoubleProperty maxCellWidth = new SimpleDoubleProperty(300);

  /**
   * The maximum height a cell is allowed to become.
   */
  public final DoubleProperty maxCellHeightProperty() { return maxCellHeight; }
  public final double getMaxCellHeight() { return maxCellHeight.get(); }
  private final DoubleProperty maxCellHeight = new SimpleDoubleProperty(200);

  /**
   * The horizontal location of the center of the carousel expressed as a fraction of
   * the view width.  0.0 represents the left edge of the view, 0.5 the center and 1.0
   * the right edge.<p>
   *
   * Defaults to 0.5.
   *
   * @return the center shift property
   */
  public final DoubleProperty centerPositionProperty() { return centerPosition; }
  public final double getCenterPosition() { return centerPosition.get(); }
  private final DoubleProperty centerPosition = new SimpleDoubleProperty(0.5);

  /**
   * The vertical alignment of the camera with respect to the carousel, expressed as
   * a fraction of its height.  An alignment of 0.5 is the carousel center, 1.0 its
   * bottom, and 0.0 its top.<p>
   *
   * Defaults to 0.5.
   *
   * @return the view alignment property
   */
  public final DoubleProperty viewAlignmentProperty() { return viewAlignment; }
  public final double getViewAlignment() { return viewAlignment.get(); }
  private final DoubleProperty viewAlignment = new SimpleDoubleProperty(0.5);

  private final List<InvalidationListener> invalidationListeners = new ArrayList<>();

  public Layout() {
    densityProperty().addListener(this::triggerInvalidation);
    cellAlignmentProperty().addListener(this::triggerInvalidation);
    reflectionEnabledProperty().addListener(this::triggerInvalidation);
    clipReflectionsProperty().addListener(this::triggerInvalidation);
    maxCellWidthProperty().addListener(this::triggerInvalidation);
    maxCellHeightProperty().addListener(this::triggerInvalidation);
    centerPositionProperty().addListener(this::triggerInvalidation);
    viewAlignmentProperty().addListener(this::triggerInvalidation);
  }

  public void addListener(InvalidationListener listener) {
    invalidationListeners.add(listener);
  }

  public void removeListener(InvalidationListener listener) {
    invalidationListeners.remove(listener);
  }

  protected void triggerInvalidation(Observable obs) {
    for(InvalidationListener invalidationListener : invalidationListeners) {
      invalidationListener.invalidated(obs);
    }
  }

  public abstract CellIterator renderCellIterator(double fractionalIndex, Orientation orientation, Dimension2D size, int maxItems);

  /**
   * Can be overriden to do some post processing on cells, like changing their opacity.
   *
   * @param iterator a {@link CellIterator}, never null
   * @param cell a {@link CarouselListCell}, never null
   * @param fractionalIndex the exact index of the cell, where 0 is the center cell and fractional values represent in-between locations
   */
  protected void postProcessCell(CellIterator iterator, CarouselListCell<?> cell, double fractionalIndex) {
    // No default implementation
  }

}

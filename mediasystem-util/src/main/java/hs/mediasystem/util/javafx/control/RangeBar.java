package hs.mediasystem.util.javafx.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.layout.StackPane;

public class RangeBar extends StackPane {
  private final DoubleProperty min = new SimpleDoubleProperty(0.0);
  public DoubleProperty minProperty() { return min; }

  private final DoubleProperty max = new SimpleDoubleProperty(1.0);
  public DoubleProperty maxProperty() { return max; }

  private final DoubleProperty origin = new SimpleDoubleProperty(0.0);
  public DoubleProperty originProperty() { return origin; }

  private final DoubleProperty value = new SimpleDoubleProperty(0.0);
  public DoubleProperty valueProperty() { return value; }

  private final StackPane track = new StackPane();
  private final StackPane range = new StackPane();
  private final StackPane thumb = new StackPane();

  public RangeBar(double min, double max, double origin) {
    getStylesheets().add(getClass().getResource("range-bar.css").toExternalForm());
    getStyleClass().add("range-bar");

    this.min.set(min);
    this.max.set(max);
    this.origin.set(origin);

    track.getStyleClass().add("track");
    range.getStyleClass().add("range");
    thumb.getStyleClass().add("thumb");

    getChildren().addAll(track, range, thumb);

    InvalidationListener invalidationListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable paramObservable) {
        requestLayout();
      }
    };

    this.min.addListener(invalidationListener);
    this.max.addListener(invalidationListener);
    this.origin.addListener(invalidationListener);
    this.value.addListener(invalidationListener);
  }

  public RangeBar() {
    this(0.0, 1.0, 0.0);
  }

  @Override
  protected double computeMinHeight(double width) {
    return computeMaxHeight(width);
  }

  @Override
  protected double computeMaxHeight(double width) {
    return track.prefHeight(width) + getInsets().getTop() + getInsets().getBottom();
  }

  @Override
  protected void layoutChildren() {
    Insets insets = getInsets();

    double left = insets.getLeft();
    double top = insets.getTop();
    double trackWidth = getWidth() - getInsets().getLeft() - getInsets().getRight();
    double height = getHeight() - getInsets().getTop() - getInsets().getBottom();

    layoutInArea(track, left, top, trackWidth, height, 0, null, true, true, HPos.CENTER, VPos.CENTER);

    double trackHorizontalInsets = track.getInsets().getLeft() + track.getInsets().getRight();

    trackWidth -= trackHorizontalInsets;

    double rangeWidth = trackWidth / (max.get() - min.get()) * Math.abs(origin.get() - value.get());
    double rangeStart = trackWidth / (max.get() - min.get()) * (Math.min(origin.get(), value.get()) - min.get());

    layoutInArea(range, left + rangeStart, top, rangeWidth + trackHorizontalInsets, height, 0, null, true, true, HPos.CENTER, VPos.CENTER);

    double thumbWidth = track.getHeight();
    double thumbPosition = trackWidth / (max.get() - min.get()) * (value.get() - min.get()) - thumbWidth / 2;

    layoutInArea(thumb, left + thumbPosition + track.getInsets().getLeft(), insets.getTop(), thumbWidth, thumbWidth, 0, null, true, true, HPos.CENTER, VPos.CENTER);
  }
}

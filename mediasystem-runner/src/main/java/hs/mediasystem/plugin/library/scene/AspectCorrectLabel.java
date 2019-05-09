package hs.mediasystem.plugin.library.scene;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;

/**
 * Note: aspect correct includes borders and padding, so the content itself may not be aspect correct!
 */
public class AspectCorrectLabel extends Label {
  private final double aspectRatio;
  private final Orientation orientation;
  private final double baseWidth;
  private final double baseHeight;

  /**
   *
   * @param text
   * @param aspectRatio
   * @param orientation
   * @param baseWidth the width to return if no height was given to calculate aspect with
   * @param baseHeight the height to return if no width was given to calculate aspect with
   */
  public AspectCorrectLabel(String text, double aspectRatio, Orientation orientation, double baseWidth, double baseHeight) {
    super(text);

    this.aspectRatio = aspectRatio;
    this.orientation = orientation;
    this.baseWidth = baseWidth;
    this.baseHeight = baseHeight;
  }

  @Override
  public Orientation getContentBias() {
    return orientation;
  }

  @Override
  protected double computePrefWidth(double height) {
    if(height > 0) {
      return snapSizeX(height / aspectRatio);
    }

    return baseWidth;
  }

  @Override
  protected double computePrefHeight(double width) {
    if(width > 0) {
      return snapSizeY(width * aspectRatio);
    }

    return baseHeight;
  }

  @Override
  protected double computeMaxWidth(double height) {
    if(height > 0) {
      return computePrefWidth(height);
    }

    return 10000;
  }

  @Override
  protected double computeMaxHeight(double width) {
    if(width > 0) {
      return computePrefHeight(width);
    }

    return 10000 * aspectRatio;
  }
}

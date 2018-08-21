package hs.mediasystem.plugin.library.scene;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;

public class AspectCorrectLabel extends Label {
  private final double aspectRatio;
  private final Orientation orientation;

  public AspectCorrectLabel(String text, double aspectRatio, Orientation orientation) {
    super(text);

    this.aspectRatio = aspectRatio;
    this.orientation = orientation;
  }

  @Override
  public Orientation getContentBias() {
    return orientation;
  }

  @Override
  protected double computePrefWidth(double height) {
    if(height > 0) {
      return height / aspectRatio;
    }

    return super.computePrefWidth(height);
  }

  @Override
  protected double computePrefHeight(double width) {
    if(width > 0) {
      return width * aspectRatio;
    }

    return super.computePrefHeight(width);
  }

  @Override
  protected double computeMaxWidth(double height) {
    if(height > 0) {
      return height / aspectRatio;
    }

    return 10000;
  }

  @Override
  protected double computeMaxHeight(double width) {
    if(width > 0) {
      return width * aspectRatio;
    }

    return 10000 * aspectRatio;
  }
}

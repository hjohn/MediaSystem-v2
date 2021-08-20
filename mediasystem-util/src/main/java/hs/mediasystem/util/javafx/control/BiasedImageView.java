package hs.mediasystem.util.javafx.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.image.Image;

/**
 * An image view with support for content bias.
 */
public class BiasedImageView extends AbstractImageView {
  private final BooleanProperty preserveRatio = new SimpleBooleanProperty(true);
  public BooleanProperty preserveRatioProperty() { return preserveRatio; }
  public final boolean isPreserveRatio() { return this.preserveRatio.get(); }
  public final void setPreserveRatio(boolean preserveRatio) { this.preserveRatio.set(preserveRatio); }

  private double maxRatio = Double.MAX_VALUE;
  private double minRatio = 0;
  private double expectedAspectRatio;  // Used when there is no place holder and no image yet for layout size calculations
  private Orientation orientation = Orientation.HORIZONTAL;

  @Override
  public Orientation getContentBias() {
    return orientation;
  }

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  public void setMinRatio(double minRatio) {
    this.minRatio = minRatio;
  }

  public void setMaxRatio(double maxRatio) {
    this.maxRatio = maxRatio;
  }

  public BiasedImageView(Node placeHolder, double expectedAspectRatio) {
    super(placeHolder);

    this.expectedAspectRatio = expectedAspectRatio;
  }

  public BiasedImageView(Node placeHolder) {
    this(placeHolder, 16.0 / 9);
  }

  public BiasedImageView(double expectedAspectRatio) {
    this(null, expectedAspectRatio);
  }

  public BiasedImageView() {
    this(null);
  }

  @Override
  protected void layoutChildren() {
    Insets insets = effectRegion.getInsets();

    double insetsWidth = insets.getLeft() + insets.getRight();
    double insetsHeight = insets.getTop() + insets.getBottom();

    double areaWidth = getWidth() - insetsWidth;
    double areaHeight = getHeight() - insetsHeight;

    double fitWidth = areaWidth;
    double fitHeight = areaHeight;

    Image image = imageView.getImage();

    if(image != null && isPreserveRatio()) {
      double imageWidth = image.getWidth();
      double imageHeight = image.getHeight();
      double w = snapSizeX(imageWidth * areaHeight / imageHeight);

      if(w <= areaWidth) {
        fitWidth = w;
      }
      else {
        fitHeight = snapSizeY(imageHeight * areaWidth / imageWidth);
      }
    }

    imageView.setFitWidth(fitWidth);
    imageView.setFitHeight(fitHeight);

    layoutInternalNodes();
  }

  private double imageWidth;
  private double imageHeight;
  private double ratio;

  private void calculateImageSize() {
    Image image = imageView.getImage();

    if(image != null) {
      imageWidth = image.getWidth();
      imageHeight = image.getHeight();
      ratio = Math.max(minRatio, Math.min(maxRatio, imageWidth / imageHeight));
    }
    else {
      imageWidth = 0;
      imageHeight = 0;
      ratio = 1;
    }
  }

  private double calcWidthGivenHeight(double height) {
    if(height < 0) {
      throw new IllegalArgumentException("height cannot be negative");
    }

    calculateImageSize();

    Insets insets = effectRegion.getInsets();

    double insetsWidth = insets.getLeft() + insets.getRight();
    double insetsHeight = insets.getTop() + insets.getBottom();
    double r = imageWidth != 0 && imageHeight != 0 ? ratio : expectedAspectRatio;

    return snapSizeX((height - insetsHeight) * r + insetsWidth);
  }

  private double calcHeightGivenWidth(double width) {
    if(width < 0) {
      throw new IllegalArgumentException("width cannot be negative: " + width);
    }

    calculateImageSize();

    Insets insets = effectRegion.getInsets();

    double insetsWidth = insets.getLeft() + insets.getRight();
    double insetsHeight = insets.getTop() + insets.getBottom();
    double r = imageWidth != 0 && imageHeight != 0 ? ratio : expectedAspectRatio;

    return snapSizeY((width - insetsWidth) / r + insetsHeight);
  }

  @Override
  protected double computePrefWidth(double height) {
    calculateImageSize();

    if(height < 0) {
      if(imageWidth != 0 && imageHeight != 0) {
        return imageWidth;
      }
      if(placeHolder != null) {
        return placeHolder.prefWidth(height);
      }

      return 30;
    }

    return calcWidthGivenHeight(height);
  }

  @Override
  protected double computePrefHeight(double width) {
    calculateImageSize();

    if(width < 0) {
      if(imageWidth != 0 && imageHeight != 0) {
        return imageHeight;
      }
      if(placeHolder != null) {
        return placeHolder.prefHeight(width);
      }

      return 30;
    }

    return calcHeightGivenWidth(width);
  }

  @Override
  protected double computeMaxWidth(double height) {
    if(height >= 0 && height != Double.MAX_VALUE && getContentBias() == Orientation.VERTICAL) {
      return calcWidthGivenHeight(height);
    }

    return Double.MAX_VALUE;
  }

  @Override
  protected double computeMaxHeight(double width) {
    if(width >= 0 && width != Double.MAX_VALUE && getContentBias() == Orientation.HORIZONTAL) {
      return calcHeightGivenWidth(width);
    }

    return Double.MAX_VALUE;
  }
}

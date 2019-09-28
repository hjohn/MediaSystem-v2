package hs.mediasystem.util.javafx.control;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class ScaledImageView extends Region {
  private final ImageView imageView = new ImageView();
  private final StackPane effectRegion = new StackPane();
  private final StackPane overlayRegion = new StackPane();

  private final BooleanProperty zoom = new SimpleBooleanProperty(false);  // true = scale to cover whole area when preserveRatio is true
  public BooleanProperty zoomProperty() { return zoom; }
  public final boolean isZoom() { return zoom.get(); }
  public final void setZoom(boolean zoom) { this.zoom.set(zoom); }

  private final ObjectProperty<Pos> alignment = new SimpleObjectProperty<>(Pos.TOP_LEFT);
  public ObjectProperty<Pos> alignmentProperty() { return alignment; }
  public final Pos getAlignment() { return this.alignment.get(); }
  public final void setAlignment(Pos pos) { this.alignment.set(pos); }

  private final BooleanProperty preserveRatio = new SimpleBooleanProperty(true);
  public BooleanProperty preserveRatioProperty() { return preserveRatio; }
  public final boolean isPreserveRatio() { return this.preserveRatio.get(); }
  public final void setPreserveRatio(boolean preserveRatio) { this.preserveRatio.set(preserveRatio); }

  private final BooleanBinding imagePresent = imageView.imageProperty().isNotNull();
  private final Node placeHolder;

  public ScaledImageView(Node placeHolder) {
    this.placeHolder = placeHolder;

    getChildren().add(effectRegion);  // effect region before image view so a background color won't overlay over the image
    getChildren().add(imageView);
    getChildren().add(overlayRegion);

    /*
     * Ratios are preserved by this Region instead of having ImageView do it, because ImageView will sometimes size a pixel smaller than expected depending on whether
     * it takes fitWidth or fitHeight into account first.  By having the Region do it the image is simply stretched for one pixel, hiding ugly artifacts when
     * a background color or border is applied to the Region and the image doesn't quite fit it.
     *
     * For example: when fitWidth/fitHeight are set to 800x600 but the image is 801x601 then the actual size (to preserve ratio as close as possible) might end up not
     * quite filling the 800x600 area but instead results in an image of 800x599 or 799x600.  By not having the ImageView preserve the ratio, 800x600 is achieved
     * exactly with an unnoticable stretch of 1 pixel in one of the axis.
     */

    imageView.setPreserveRatio(false);

    getStyleClass().add("scaled-image-view");

    effectRegion.getStyleClass().add("image-view");
    overlayRegion.getStyleClass().add("image-view-overlay");

    imagePresent.addListener(obs -> {
      Platform.runLater(() -> requestLayout());  // Request layout when image presence changes; layout makes image visible or not, to prevent flashing the image at the wrong size
    });

  //  imageView.visibleProperty().bind(imagePresent);

  //  effectRegion.visibleProperty().bind(Bindings.createBooleanBinding(() -> placeHolder != null || imageView.getImage() != null, imageView.imageProperty()));  // prevents effect region from being visible if there is no image and no placeholder

    if(placeHolder != null) {
      effectRegion.getChildren().add(placeHolder);
      placeHolder.getStyleClass().add("place-holder");
      placeHolder.visibleProperty().bind(imageView.imageProperty().isNull());
    }

//    backgroundLoadingBinding.addListener((ov, old, isLoading) -> {
//      System.out.println("isLoading = " + isLoading + " for " + getImage());
//      if(isLoading != null && !isLoading) {
//        if(getOpacity() == 0) {
//          new Timeline(
//            new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0)),
//            new KeyFrame(Duration.seconds(1), new KeyValue(opacityProperty(), 1))
//          ).play();
//        }
//      }
//      else {
//        setOpacity(0);
//      }
//    });
  }

  public StackPane getOverlayRegion() {
    return overlayRegion;
  }

  public ScaledImageView() {
    this(null);
  }

  @Override
  public String getTypeSelector() {
    return "ScaledImageView";  // To prevent anonymous inner classes messing this up
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

    imageView.setVisible(image != null);
    effectRegion.setVisible(image != null || placeHolder != null);

    if(image != null && isPreserveRatio()) {
      double imageWidth = image.getWidth();
      double imageHeight = image.getHeight();

      double w = imageWidth * areaHeight / imageHeight;

      if(w < areaWidth) {
        fitWidth = w;
      }
      else {
        fitHeight = imageHeight * areaWidth / imageWidth;
      }
    }

    if(zoom.get() && isPreserveRatio() && (areaWidth != fitWidth || areaHeight != fitHeight)) {
      imageView.setFitWidth(areaWidth);
      imageView.setFitHeight(areaHeight);

      /*
       * Need to define a viewport to make sure the image fills the entire available area while still
       * preserving ratio.
       */

      if(image != null) {
        double horizontalRatio = areaWidth / image.getWidth();
        double verticalRatio = areaHeight / image.getHeight();

        if(horizontalRatio > verticalRatio) {
          double viewportWidth = image.getWidth();
          double viewportHeight = areaHeight / horizontalRatio;

          double yOffset = alignment.get().getVpos() == VPos.BOTTOM ? image.getHeight() - viewportHeight :
                           alignment.get().getVpos() == VPos.CENTER ? (image.getHeight() - viewportHeight) / 2 : 0;

          imageView.setViewport(new Rectangle2D(0, yOffset, viewportWidth, viewportHeight));
        }
        else {
          double viewportWidth = areaWidth / verticalRatio;
          double viewportHeight = image.getHeight();

          double xOffset = alignment.get().getHpos() == HPos.RIGHT ? image.getWidth() - viewportWidth :
                           alignment.get().getHpos() == HPos.CENTER ? (image.getWidth() - viewportWidth) / 2 : 0;

          imageView.setViewport(new Rectangle2D(xOffset, 0, viewportWidth, viewportHeight));
        }
      }
    }
    else {
      imageView.setViewport(null);
      imageView.setFitWidth(fitWidth);
      imageView.setFitHeight(fitHeight);
    }

    Bounds bounds = imageView.getLayoutBounds();

    layoutInArea(imageView, insets.getLeft(), insets.getTop(), getWidth() - insetsWidth, getHeight() - insetsHeight, 0, alignment.get().getHpos(), alignment.get().getVpos());

    effectRegion.setMinWidth(Math.round(bounds.getWidth()) + insetsWidth);
    effectRegion.setMinHeight(Math.round(bounds.getHeight()) + insetsHeight);
    effectRegion.setMaxWidth(effectRegion.getMinWidth());
    effectRegion.setMaxHeight(effectRegion.getMinHeight());

    layoutInArea(effectRegion, 0, 0, getWidth(), getHeight(), 0, alignment.get().getHpos(), alignment.get().getVpos());

    overlayRegion.setMinWidth(Math.round(bounds.getWidth()) + insetsWidth);
    overlayRegion.setMinHeight(Math.round(bounds.getHeight()) + insetsHeight);
    overlayRegion.setMaxWidth(overlayRegion.getMinWidth());
    overlayRegion.setMaxHeight(overlayRegion.getMinHeight());

    layoutInArea(overlayRegion, 0, 0, getWidth(), getHeight(), 0, alignment.get().getHpos(), alignment.get().getVpos());

    // System.out.println(">>> LayoutChildren: " + this.hashCode() + " image = " + image + "; ; fw/fh = " + fitWidth + "/" + fitHeight + "; w/h = " + getWidth() + "/" + getHeight() + ", layoutInArea = " + insets.getLeft() + "," + insets.getRight() + "," + (getWidth() - insetsWidth) + "," + (getHeight() - insetsHeight) + ": result: " + effectRegion.getWidth());
  }

  private double imageWidth;
  private double imageHeight;

  private void calculateImageSize() {
    Image image = imageView.getImage();

    if(image != null) {
      imageWidth = image.getWidth();
      imageHeight = image.getHeight();
    }
    else {
      imageWidth = 0;
      imageHeight = 0;
    }
  }

  @Override
  protected double computePrefWidth(double height) {
    return 10;
  }

  @Override
  protected double computePrefHeight(double width) {
    return 10;
  }

//  @Override
//  public Orientation getContentBias() {
//    return Orientation.HORIZONTAL;
//  }

//  @Override
//  protected double computePrefWidth(double height) {
//    calculateImageSize();
//
//    System.err.println("computePrefWidth called with : " + height + " : w/h = " + getWidth() + " - " + getHeight());
//    if(height > 0 && isPreserveRatio()) {
//      return imageWidth * height / imageHeight;
//    }
//
//    return 30;
//  }
//
//  @Override
//  protected double computePrefHeight(double width) {
//    calculateImageSize();
//    System.err.println("computePrefHeight called with : " + width + " : w/h = " + getWidth() + " - " + getHeight());
//    if(width > 0 && isPreserveRatio()) {
//      return imageHeight * width / imageWidth;
//    }
//
//    return 30;
//  }
//
//  @Override
//  protected double computeMaxWidth(double height) {
//    System.err.println("computeMaxWidth called with : " + height + " : w/h = " + getWidth() + " - " + getHeight());
//    if(height > 0) {
//      return computePrefWidth(height);
//    }
//    return Double.MAX_VALUE;
//  }
//
//  @Override
//  protected double computeMaxHeight(double width) {
//    System.err.println("computeMaxHeight called with : " + width + " : w/h = " + getWidth() + " - " + getHeight());
//    if(width > 0) {
//      return computePrefHeight(width);
//    }
//    return Double.MAX_VALUE;
//  }

  public final ObjectProperty<Image> imageProperty() { return imageView.imageProperty(); }
  public final Image getImage() { return imageView.getImage(); }
  public final void setImage(Image image) { imageView.setImage(image); }

//  public final BooleanProperty preserveRatioProperty() { return imageView.preserveRatioProperty(); }
//  public final boolean isPreserveRatio() { return imageView.isPreserveRatio(); }
//  public final void setPreserveRatio(boolean preserveRatio) { imageView.setPreserveRatio(preserveRatio); }

  public final BooleanProperty smoothProperty() { return imageView.smoothProperty(); }
  public final boolean isSmooth() { return imageView.isSmooth(); }
  public final void setSmooth(boolean smooth) { imageView.setSmooth(smooth); }
}

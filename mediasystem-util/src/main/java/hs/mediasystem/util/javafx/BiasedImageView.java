package hs.mediasystem.util.javafx;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class BiasedImageView extends Region {
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

  private Orientation orientation = Orientation.HORIZONTAL;
  private double maxRatio = Double.MAX_VALUE;
  private double minRatio = 0;

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  public void setMinRatio(double minRatio) {
    this.minRatio = minRatio;
  }

  public void setMaxRatio(double maxRatio) {
    this.maxRatio = maxRatio;
  }

  public BiasedImageView(Node placeHolder) {
    this.placeHolder = placeHolder;

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

    getChildren().add(effectRegion);  // effect region before image view so a background color won't overlay over the image

    if(placeHolder != null) {
      getChildren().add(placeHolder);

      placeHolder.getStyleClass().add("place-holder");
      effectRegion.visibleProperty().set(false);
    }

    getChildren().add(imageView);
    getChildren().add(overlayRegion);
  }

  public StackPane getOverlayRegion() {
    return overlayRegion;
  }

  public BiasedImageView() {
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
    effectRegion.setVisible(image != null);
    if(placeHolder != null) {
      placeHolder.setVisible(image == null);
    }

    if(image != null && isPreserveRatio()) {
      double imageWidth = image.getWidth();
      double imageHeight = image.getHeight();
      double w = snapSize(imageWidth * areaHeight / imageHeight);

      if(w <= areaWidth) {
        fitWidth = w;
      }
      else {
        fitHeight = snapSize(imageHeight * areaWidth / imageWidth);
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

    if(placeHolder != null) {
      layoutInArea(placeHolder, 0, 0, getWidth(), getHeight(), 0, alignment.get().getHpos(), alignment.get().getVpos());
    }
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

  @Override
  protected double computeMinWidth(double height) {
  //System.out.println("----computeMinWidth called with " + height);
    return computePrefWidth(height);
  }

  @Override
  protected double computeMinHeight(double width) {
  //System.out.println("----computeMinHeight called with " + width);
    return computePrefHeight(width);
  }

//  @Override
//  protected double computePrefWidth(double height) {
//    System.out.println("----computePrefWidth called with " + height);
//    return 10;
//  }

//  @Override
//  protected double computePrefHeight(double width) {
//    //System.out.println("----computePrefHeight called with " + width);
//    return 10;
//  }

  @Override
  public Orientation getContentBias() {
    return orientation;
  }

  @Override
  protected double computePrefWidth(double height) {
    calculateImageSize();

    //System.err.println("---computePrefWidth called with : " + height + " : w/h=" + getWidth() + "x" + getHeight() + " ; image=" + imageWidth + "x" + imageHeight);
    if(height > 0 && height < 100000 && isPreserveRatio()) {
      if(imageWidth != 0 && imageHeight != 0) {
        //System.err.println("----------> " + (imageWidth * height / imageHeight));
        return snapSize(height * ratio);
      }
      if(placeHolder != null) {
        return placeHolder.prefWidth(height);
      }
    }

    return 30;
  }

  @Override
  protected double computePrefHeight(double width) {
    calculateImageSize();
    if(width > 0 && width < 100000 && isPreserveRatio()) {
      if(imageWidth != 0 && imageHeight != 0) {
  //      System.out.println("----computePrefHeight called with : " + width + " : w/h = " + getWidth() + "x" + getHeight() + " ; image=" + imageWidth + "x" + imageHeight + " ---> " + (imageHeight * width / imageWidth));
        return snapSize(width / ratio);
      }
      if(placeHolder != null) {
        return placeHolder.prefHeight(width);
      }
    }

    return 30;
  }
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

package hs.mediasystem.util.javafx.control;

import hs.mediasystem.util.javafx.Nodes;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.util.Duration;

/**
 *
 * Contains four children, stacked on top of each other:
 *
 *  "image-view" - The effect region
 *  "place-holder" - Optional place holder, which will be sized just like the image would
 *  "scaled-image-view" - The actual image
 *  "image-view-overlay" - A region that is placed on top of the image
 *
 * The effect and overlay regions always exactly match the size of the image (excluding
 * borders).
 *
 * The place holder should be around the same size as the expected image to prevent "jumps",
 * if possible.
 *
 * To add borders to the image, use the effect region.  To give the same borders to the
 * place holder (as the effect region and placeholder are never shown at the same time)
 * use a border around the placeholder with a negative insets value, equal to the size
 * of the border used for the effect region.
 */
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
  private double expectedAspectRatio;  // Used when there is no place holder and no image yet for layout size calculations
  private boolean visibleForOneFrame;  // If true, fade-in image, if false show image immediately

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
    this.placeHolder = placeHolder;
    this.expectedAspectRatio = expectedAspectRatio;

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

    effectRegion.getStyleClass().add("image-view-effect");
    overlayRegion.getStyleClass().add("image-view-overlay");

    imagePresent.addListener(obs -> {
      Platform.runLater(() -> requestLayout());  // Request layout when image presence changes; layout makes image visible or not, to prevent flashing the image at the wrong size
    });

    setupFadeIn(placeHolder);

    getChildren().add(effectRegion);  // effect region before image view so a background color won't overlay over the image

    if(placeHolder != null) {
      getChildren().add(placeHolder);

      placeHolder.getStyleClass().add("place-holder");
    }

    getChildren().add(imageView);
    getChildren().add(overlayRegion);
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

  private void setupFadeIn(Node placeHolder) {
    imageView.setOpacity(0);

    Nodes.showingStream(this).subscribe(v -> {
      Platform.runLater(() -> {
        visibleForOneFrame = v;
      });

      if(!v) {
        visibleForOneFrame = false;
      }
    });

    Timeline timeline;

    if(placeHolder == null) {
      timeline = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(imageView.opacityProperty(), 0)),
        new KeyFrame(Duration.seconds(1), new KeyValue(imageView.opacityProperty(), 1))
      );
    }
    else {
      effectRegion.setOpacity(1);

      timeline = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(imageView.opacityProperty(), 0), new KeyValue(placeHolder.opacityProperty(), 1)),
        new KeyFrame(Duration.seconds(1), new KeyValue(imageView.opacityProperty(), 1), new KeyValue(placeHolder.opacityProperty(), 0))
      );
    }

    imageProperty().addListener((obs, old, current) -> {
      if(current != null) {
        if(visibleForOneFrame && imageView.getOpacity() == 0) {
          timeline.play();
        }
        else {
          imageView.setOpacity(1);
          effectRegion.setOpacity(1);
          if(placeHolder != null) {
            placeHolder.setOpacity(0);
          }
        }
      }
      else {
        // Reset opacities when image becomes null, so a clean fade-in is possible later
        timeline.stop();  // Stop timeline, or it may override stuff if still playing

        if(placeHolder != null) {
          placeHolder.setOpacity(1);
        }

        imageView.setOpacity(0);
      }
    });
  }

  public StackPane getOverlayRegion() {
    return overlayRegion;
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

//    imageView.setVisible(image != null);
//    effectRegion.setVisible(image != null);
//    if(placeHolder != null) {
//      placeHolder.setVisible(image == null);
//    }

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

    layoutInArea(imageView, insets.getLeft(), insets.getTop(), getWidth() - insetsWidth, getHeight() - insetsHeight, 0, alignment.get().getHpos(), alignment.get().getVpos());

    /*
     * Get actual bounds of ImageView, and force this size on the effect and overlay regions by setting
     * their min and max sizes.  Since these regions share the same alignment, it should match with the
     * actual image position, allowing accurate borders and overlays over the image.
     */

    double actualWidth = imageView.getFitWidth();
    double actualHeight = imageView.getFitHeight();

    effectRegion.setMinWidth(actualWidth + insetsWidth);
    effectRegion.setMinHeight(actualHeight + insetsHeight);
    effectRegion.setMaxWidth(effectRegion.getMinWidth());
    effectRegion.setMaxHeight(effectRegion.getMinHeight());

    if(placeHolder != null) {
      layoutInArea(placeHolder, insets.getLeft(), insets.getTop(), getWidth() - insetsWidth, getHeight() - insetsHeight, 0, alignment.get().getHpos(), alignment.get().getVpos());
    }
    layoutInArea(effectRegion, 0, 0, getWidth(), getHeight(), 0, alignment.get().getHpos(), alignment.get().getVpos());

    overlayRegion.setMinWidth(actualWidth + insetsWidth);
    overlayRegion.setMinHeight(actualHeight + insetsHeight);
    overlayRegion.setMaxWidth(overlayRegion.getMinWidth());
    overlayRegion.setMaxHeight(overlayRegion.getMinHeight());

    layoutInArea(overlayRegion, 0, 0, getWidth(), getHeight(), 0, alignment.get().getHpos(), alignment.get().getVpos());
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

  /*
   * Min width/height for an image is basically zero, so return
   * small values from these.  It's best to ignore the input
   * bias values as (when taking ratio into account) they will
   * give a totally wrong impression of how small the image can be.
   */

  @Override
  protected double computeMinWidth(double height) {
  //System.out.println("----computeMinWidth called with " + height);
//    if(height != -1 && getContentBias() == Orientation.VERTICAL) {
//      return calcWidthGivenHeight(height);
//    }

    return 30;
  }

  @Override
  protected double computeMinHeight(double width) {
  //System.out.println("----computeMinHeight called with " + width);
//    if(width != -1 && getContentBias() == Orientation.HORIZONTAL) {
//      return calcHeightGivenWidth(width);
//    }

    return 30;
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
  public Orientation getContentBias() {
    return orientation;
  }

  @Override
  protected double computePrefWidth(double height) {
    double w = computePrefWidthInternalNew(height);

//    System.out.println("==computePrefWidth(" + height + ") -> " + w);

    return w;
  }

  private double computePrefWidthInternalNew(double height) {
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
    double h = computePrefHeightInternalNew(width);

//    System.out.println("==computePrefHeight(" + width + ") -> " + h);

    return h;
  }

  private double computePrefHeightInternalNew(double width) {
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

  public final ObjectProperty<Image> imageProperty() { return imageView.imageProperty(); }
  public final Image getImage() { return imageView.getImage(); }
  public final void setImage(Image image) { imageView.setImage(image); }

  public final BooleanProperty smoothProperty() { return imageView.smoothProperty(); }
  public final boolean isSmooth() { return imageView.isSmooth(); }
  public final void setSmooth(boolean smooth) { imageView.setSmooth(smooth); }
}

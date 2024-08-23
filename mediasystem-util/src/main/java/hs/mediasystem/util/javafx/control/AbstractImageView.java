package hs.mediasystem.util.javafx.control;

import hs.mediasystem.util.javafx.base.Nodes;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Abstract base class for {@link ImageView} containers containing basic functionality
 * applying to all image view containers.<p>
 *
 * Structure (in back to front order)<p>
 * <li>image-view-effect: a {@link StackPane} which is layered below the actual image and
 * which content area takes on the exact same size as the image. The insets set on this layer
 * therefore are not covered by the image to make it possible to add borders.</li>
 * <li>place-holder: a user supplied {@link Node} which will be shown when no image is
 * currently set.</li>
 * <li>image-view: an {@link ImageView} containing the actual image. As {@link ImageView}s
 * are simple nodes, they can't have insets but can have an effect applied. The
 * size of the {@link ImageView} is determined by the contained image, minus any insets of
 * the effect layer.</li>
 * <li>image-view-overlay: a {@link StackPane} which is overlayed on top of the image of the
 * exact same size as the image-view-effect pane. This layer can be used to overlay nodes on
 * top of the image.</li><p>
 *
 * This class extends {@link Region} and can have its own styles applied. However, styles
 * applied to this region will not follow the image dimensions as this region can be larger
 * when the image has its aspect ratio preserved.<p>
 *
 * The place holder should be around the same size as the expected image to prevent "jumps",
 * if possible.<p>
 *
 * To add borders to the image, use the effect region.  To give the same borders to the
 * place holder (as the effect region and placeholder are never shown at the same time)
 * use a border around the placeholder with a negative insets value, equal to the size
 * of the border used for the effect region.
 */
public abstract class AbstractImageView extends Region {
  protected final ImageView imageView = new ImageView();
  protected final StackPane effectRegion = new StackPane();
  protected final StackPane overlayRegion = new StackPane();

  private final BooleanBinding imagePresent = imageView.imageProperty().isNotNull();
  protected final Node placeHolder;

  private boolean visibleForOneFrame;  // If true, fade-in image, if false show image immediately

  public AbstractImageView(Node placeHolder) {
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

    imageView.setId("image-view");
    effectRegion.getStyleClass().add("image-view-effect");
    overlayRegion.getStyleClass().add("image-view-overlay");

    imageProperty().addListener(obs -> requestLayout());
    imagePresent.addListener(obs -> {
      Platform.runLater(() -> requestLayout());  // Request layout when image presence changes; layout makes image visible or not, to prevent flashing the image at the wrong size
    });

    setupFadeIn(placeHolder);

    getChildren().add(effectRegion);  // effect region before image view so a background color won't overlay over the image

    if(placeHolder != null) {
      getChildren().add(placeHolder);

      placeHolder.getStyleClass().add("place-holder");
      placeHolder.setManaged(false);
    }

    getChildren().add(imageView);
    getChildren().add(overlayRegion);

    effectRegion.setManaged(false);
    imageView.setManaged(false);
    overlayRegion.setManaged(false);
  }

  public AbstractImageView() {
    this(null);
  }

  private void setupFadeIn(Node placeHolder) {
    imageView.setOpacity(0);

    Nodes.showing(this).subscribe(v -> {
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

  protected void layoutInternalNodes(double imageWidth, double imageHeight) {
    Insets insets = effectRegion.getInsets();

    double insetsWidth = insets.getLeft() + insets.getRight();
    double insetsHeight = insets.getTop() + insets.getBottom();

    effectRegion.setMaxWidth(imageWidth + insetsWidth);
    effectRegion.setMaxHeight(imageHeight + insetsHeight);
    overlayRegion.setMaxWidth(imageWidth + insetsWidth);
    overlayRegion.setMaxHeight(imageHeight + insetsHeight);

    layoutInArea(imageView, insets.getLeft(), insets.getTop(), getWidth() - insetsWidth, getHeight() - insetsHeight, 0, getAlignment().getHpos(), getAlignment().getVpos());

    if(placeHolder != null) {
      layoutInArea(placeHolder, insets.getLeft(), insets.getTop(), getWidth() - insetsWidth, getHeight() - insetsHeight, 0, getAlignment().getHpos(), getAlignment().getVpos());
    }

    layoutInArea(effectRegion, 0, 0, getWidth(), getHeight(), 0, getAlignment().getHpos(), getAlignment().getVpos());
    layoutInArea(overlayRegion, 0, 0, getWidth(), getHeight(), 0, getAlignment().getHpos(), getAlignment().getVpos());
  }

  /*
   * Min width/height for an image is basically zero, so return
   * small values from these.  It's best to ignore the input
   * bias values as (when taking ratio into account) they will
   * give a totally wrong impression of how small the image can be.
   */

  @Override
  protected double computeMinWidth(double height) {
    return 30;
  }

  @Override
  protected double computeMinHeight(double width) {
    return 30;
  }

  private final ObjectProperty<Pos> alignment = new SimpleObjectProperty<>(Pos.TOP_LEFT);
  public ObjectProperty<Pos> alignmentProperty() { return alignment; }
  public final Pos getAlignment() { return this.alignment.get(); }
  public final void setAlignment(Pos pos) { this.alignment.set(pos); }

  public final ObjectProperty<Image> imageProperty() { return imageView.imageProperty(); }
  public final Image getImage() { return imageView.getImage(); }
  public final void setImage(Image image) { imageView.setImage(image); }

  public final BooleanProperty smoothProperty() { return imageView.smoothProperty(); }
  public final boolean isSmooth() { return imageView.isSmooth(); }
  public final void setSmooth(boolean smooth) { imageView.setSmooth(smooth); }
}

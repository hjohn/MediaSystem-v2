package hs.mediasystem.util.javafx.control;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * A container for an {@link ImageView} which always displays an image at the
 * original aspect ratio and always fills the entire visible area by zooming
 * in to a specific part of the image controlled by the desired alignment.
 */
public class ZoomImageView extends AbstractImageView {

  public ZoomImageView(Node placeHolder) {
    super(placeHolder);
  }

  public ZoomImageView() {
    super(null);
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

    if(image != null) {
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

    imageView.setFitWidth(areaWidth);
    imageView.setFitHeight(areaHeight);

    if(areaWidth != fitWidth || areaHeight != fitHeight) {

      /*
       * Need to define a viewport to make sure the image fills the entire available area while still
       * preserving ratio.
       */

      if(image != null) {
        double horizontalRatio = areaWidth / image.getWidth();
        double verticalRatio = areaHeight / image.getHeight();

        if(horizontalRatio > verticalRatio) {
          double viewportWidth = image.getWidth();
          double viewportHeight = Math.round(areaHeight / horizontalRatio);

          double yOffset = getAlignment().getVpos() == VPos.BOTTOM ? image.getHeight() - viewportHeight :
                           getAlignment().getVpos() == VPos.CENTER ? (image.getHeight() - viewportHeight) / 2 : 0;

          imageView.setViewport(new Rectangle2D(0, yOffset, viewportWidth, viewportHeight));
        }
        else {
          double viewportWidth = Math.round(areaWidth / verticalRatio);
          double viewportHeight = image.getHeight();

          double xOffset = getAlignment().getHpos() == HPos.RIGHT ? image.getWidth() - viewportWidth :
                           getAlignment().getHpos() == HPos.CENTER ? (image.getWidth() - viewportWidth) / 2 : 0;

          imageView.setViewport(new Rectangle2D(xOffset, 0, viewportWidth, viewportHeight));
        }
      }
    }
    else {
      imageView.setViewport(null);
    }

    layoutInternalNodes(getWidth(), getHeight());
  }

  @Override
  protected double computePrefWidth(double height) {
    return 30;
  }

  @Override
  protected double computePrefHeight(double width) {
    return 30;
  }
}

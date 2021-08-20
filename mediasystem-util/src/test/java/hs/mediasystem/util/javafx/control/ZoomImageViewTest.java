package hs.mediasystem.util.javafx.control;

import java.util.function.BiFunction;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(ApplicationExtension.class)
class ZoomImageViewTest {
  private ZoomImageView imageView;

  @Start
  void start(@SuppressWarnings("unused") Stage stage) {
    imageView = new ZoomImageView();
  }

  @ParameterizedTest
  @EnumSource(SampleSizes.class)
  void zoomShouldCalculateCorrectValues(SampleSizes data) {
    imageView.imageProperty().set(createImage(data.imageWidth, data.imageHeight));

    imageView.resize(data.width, data.height);
    imageView.layoutChildren();

    ImageView iv = (ImageView)imageView.lookup("#image-view");

    double ratio = Math.max(data.width / (double)data.imageWidth, data.height / (double)data.imageHeight);
    double wantedWidth = data.imageWidth * ratio;
    double wantedHeight = data.imageHeight * ratio;
    double expectedWidth = Math.round(Math.min(data.width, wantedWidth));
    double expectedHeight = Math.round(Math.min(data.height, wantedHeight));

    assertEquals(expectedWidth, iv.getFitWidth());
    assertEquals(expectedHeight, iv.getFitHeight());

    for(Alignment ax : Alignment.values()) {
      for(Alignment ay : Alignment.values()) {
        imageView.setAlignment(Pos.values()[ax.ordinal() + ay.ordinal() * 3]);
        imageView.layoutChildren();

        double vpW = Math.round(data.imageWidth * (expectedWidth / wantedWidth));
        double vpH = Math.round(data.imageHeight * (expectedHeight / wantedHeight));
        double vpX = ax.function.apply(vpW, data.imageWidth);
        double vpY = ay.function.apply(vpH, data.imageHeight);

        if(vpW != data.imageWidth || vpH != data.imageHeight) {
          assertEquals(new Rectangle2D(vpX, vpY, vpW, vpH), iv.getViewport());
        }
        else {
          assertNull(iv.getViewport());
        }
      }
    }
  }

  enum Alignment {
    NEAR((vpSize, imageSize) -> 0.0),
    CENTER((vpSize, imageSize) -> vpSize < imageSize ? (imageSize - vpSize) / 2 : 0),
    FAR((vpSize, imageSize) -> vpSize < imageSize ? imageSize - vpSize : 0);

    private BiFunction<Double, Integer, Double> function;

    Alignment(BiFunction<Double, Integer, Double> function) {
      this.function = function;
    }
  }

  enum SampleSizes {
    //    image-size  control-size
    Case1( 640,  480,  1000, 1000),
    Case2( 640,  480,   500, 1000),
    Case3( 640,  480,   640,  480),
    Case4( 640,  480,    64,   48),
    Case5(1920, 1080,   600,   90),
    Case6(1080, 1920,   600,   90),
    Case7(1920, 1080,    90,  600),
    Case8(1080, 1920,    90,  600),
    Case9(1920, 1080,   192,  108);

    private int imageWidth;
    private int imageHeight;
    private int width;
    private int height;

    SampleSizes(int imageWidth, int imageHeight, int width, int height) {
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
      this.width = width;
      this.height = height;
    }
  }

  public static Image createImage(int width, int height) {
    return new WritableImage(width, height);
  }
}

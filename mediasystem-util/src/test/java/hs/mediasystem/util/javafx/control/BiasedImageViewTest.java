package hs.mediasystem.util.javafx.control;

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

@ExtendWith(ApplicationExtension.class)
class BiasedImageViewTest {
  private BiasedImageView imageView;

  @Start
  void start(@SuppressWarnings("unused") Stage stage) {
    imageView = new BiasedImageView();
  }

  @ParameterizedTest
  @EnumSource(SampleSizes.class)
  void shouldCalculateCorrectValues(SampleSizes data) {
    imageView.imageProperty().set(createImage(data.imageWidth, data.imageHeight));

    imageView.resize(data.width, data.height);
    imageView.layoutChildren();

    double ratio = Math.min(data.width / (double)data.imageWidth, data.height / (double)data.imageHeight);
    double expectedWidth = Math.round(data.imageWidth * ratio);
    double expectedHeight = Math.round(data.imageHeight * ratio);

    assertEquals(expectedWidth, ((ImageView)imageView.lookup("#image-view")).getFitWidth());
    assertEquals(expectedHeight, ((ImageView)imageView.lookup("#image-view")).getFitHeight());
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

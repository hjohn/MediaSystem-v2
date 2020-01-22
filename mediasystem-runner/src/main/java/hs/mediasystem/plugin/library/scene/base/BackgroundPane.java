package hs.mediasystem.plugin.library.scene.base;

import hs.mediasystem.util.ImageHandle;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.ScaledImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class BackgroundPane extends StackPane {
  private static final long SETTLE_MILLIS = 1000;   // How long to atleast wait for a new image to be selected (when image changes rapidly)
  private static final long LOAD_MILLIS = 5000;    // How long to wait for a new image to be loaded before displaying an empty image
  private static final Duration MAX_LOAD_DURATION = Duration.millis(LOAD_MILLIS);
  private static final Image EMPTY_IMAGE = new Image(new ByteArrayInputStream(Base64.getDecoder().decode("R0lGODlhAQABAHAAACH5BAUAAAAALAAAAAABAAEAAAICRAEAOw==")));  // Tiny transparent gif
  private static final Image DEFAULT_IMAGE = new Image(BackgroundPane.class.getResourceAsStream("curtain.jpg"));

  private final ObjectProperty<ImageHandle> backdropProperty = new SimpleObjectProperty<>();
  public ObjectProperty<ImageHandle> backdropProperty() { return backdropProperty; }

  private final AsyncImageProperty wantedBackground = new AsyncImageProperty();

  private final ObjectProperty<Image> background = new SimpleObjectProperty<>(EMPTY_IMAGE);
  private final ObjectProperty<Image> newBackground = new SimpleObjectProperty<>(EMPTY_IMAGE);

  private final ScaledImageView backgroundImageView = new ScaledImageView() {{
    imageProperty().bind(background);
    setPreserveRatio(true);
    setSmooth(true);
    setAlignment(Pos.TOP_CENTER);
    setZoom(true);
  }};

  private final ScaledImageView newBackgroundImageView = new ScaledImageView() {{
    imageProperty().bind(newBackground);
    setPreserveRatio(true);
    setSmooth(true);
    setAlignment(Pos.TOP_CENTER);
    setZoom(true);
  }};

  private final EventHandler<ActionEvent> beforeBackgroundChange = new EventHandler<>() {
    @Override
    public void handle(ActionEvent event) {

      /*
       * Check if background really has changed during the SETTLE_DURATION, if not, cancel the animation.
       */

      if(isBackgroundChanged()) {
        Image image = wantedBackground.get();

        if(image == null) {
          image = DEFAULT_IMAGE;  // WORKAROUND for ImageViews being unable to handle null properly
        }

        newBackground.set(image);
      }
      else {
        timeline.stop();
      }
    }
  };

  private final EventHandler<ActionEvent> quickBackgroundChange = new EventHandler<>() {
    @Override
    public void handle(ActionEvent event) {

      /*
       * Check if background changed early to a valid Image
       */

      if(isBackgroundChanged()) {
        Image image = wantedBackground.get();

        if(image != null) {
          timeline.playFrom(MAX_LOAD_DURATION);
        }
      }
    }
  };

  private final Timeline timeline = new Timeline(
    new KeyFrame(Duration.ZERO,
      new KeyValue(backgroundImageView.opacityProperty(), 1.0),
      new KeyValue(newBackgroundImageView.opacityProperty(), 0.0)
    ),
    new KeyFrame(Duration.millis(SETTLE_MILLIS), quickBackgroundChange),
    new KeyFrame(Duration.millis(SETTLE_MILLIS + (LOAD_MILLIS - SETTLE_MILLIS) * 0.25), quickBackgroundChange),
    new KeyFrame(Duration.millis(SETTLE_MILLIS + (LOAD_MILLIS - SETTLE_MILLIS) * 0.5), quickBackgroundChange),
    new KeyFrame(Duration.millis(SETTLE_MILLIS + (LOAD_MILLIS - SETTLE_MILLIS) * 0.75), quickBackgroundChange),
    new KeyFrame(MAX_LOAD_DURATION, beforeBackgroundChange,
      new KeyValue(backgroundImageView.opacityProperty(), 1.0),
      new KeyValue(newBackgroundImageView.opacityProperty(), 0.0)
    ),
    new KeyFrame(MAX_LOAD_DURATION.add(Duration.millis(3000)),
      new KeyValue(backgroundImageView.opacityProperty(), 0.0),
      new KeyValue(newBackgroundImageView.opacityProperty(), 1.0)
    )
  );

  public BackgroundPane() {
    timeline.setOnFinished(new EventHandler<ActionEvent>() {
      @Override
      public void handle(ActionEvent event) {
        background.set(newBackground.get());
        backgroundImageView.setOpacity(1.0);
        newBackgroundImageView.setOpacity(0.0);

        if(isBackgroundChanged()) {
          timeline.play();
        }
      }
    });

    wantedBackground.imageHandleProperty().bind(backdropProperty);

    wantedBackground.imageHandleProperty().addListener(new ChangeListener<ImageHandle>() {
      @Override
      public void changed(ObservableValue<? extends ImageHandle> observable, ImageHandle oldValue, ImageHandle newValue) {

        /*
         * If the background should change and we are not currently in the change process, then start a new animation.  If we are
         * in the early stages of an animation, restart it to allow the background to 'settle'.
         */

        if(timeline.getStatus() == Animation.Status.STOPPED || timeline.getCurrentTime().lessThan(MAX_LOAD_DURATION)) {
          timeline.playFromStart();
        }
      }
    });

    getChildren().addAll(backgroundImageView, newBackgroundImageView);

    timeline.play();
  }

  private boolean isBackgroundChanged() {
    return (wantedBackground.get() == null && background.get() != DEFAULT_IMAGE) || (wantedBackground.get() != null && !wantedBackground.get().equals(background.get()));
  }
}

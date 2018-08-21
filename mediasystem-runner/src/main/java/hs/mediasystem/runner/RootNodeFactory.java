package hs.mediasystem.runner;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.ViewPort;
import hs.mediasystem.presentation.ViewPortFactory;
import hs.mediasystem.util.javafx.Containers;
import hs.mediasystem.util.javafx.Labels;
import hs.mediasystem.util.javafx.SpecialEffects;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RootNodeFactory implements NodeFactory<RootPresentation> {
  @Inject private SceneManager sceneManager;
  @Inject private ViewPortFactory viewPortFactory;

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

  @Override
  public Node create(RootPresentation presentation) {
    Node viewPort = createViewPort(presentation);

    StringProperty time = new SimpleStringProperty();
    StringProperty date = new SimpleStringProperty();
    VBox vbox = Containers.vbox("clock-box", Labels.create("clock", time), Labels.create("date", date));

    Timeline timeline = new Timeline(5,
      new KeyFrame(Duration.seconds(0), e -> {
        ZonedDateTime now = ZonedDateTime.now();

        time.set(now.format(TIME_FORMATTER));
        date.set(now.format(DATE_FORMATTER));
      }),
      new KeyFrame(Duration.seconds(0.2))
    );

    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();

    vbox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

    StackPane clockPane = new StackPane(vbox);

    clockPane.visibleProperty().bind(presentation.clockVisible);
    clockPane.getStylesheets().add(LessLoader.compile(getClass().getResource("clock-pane.less")).toExternalForm());
    clockPane.getStyleClass().add("clock-pane");

    StackPane logoPane = new StackPane(createLogo());

    logoPane.visibleProperty().bind(presentation.clockVisible);
    logoPane.getStylesheets().add(LessLoader.compile(getClass().getResource("logo-pane.less")).toExternalForm());
    logoPane.getStyleClass().add("logo-pane");

/*
    Label fpsLabel = new Label();

    StackPane fpsPane = new StackPane(fpsLabel);

    fpsPane.getStylesheets().add(LessLoader.compile(getClass().getResource("fps-pane.less")).toExternalForm());
    fpsPane.getStyleClass().add("fps-pane");

    AnimationTimer frameRateMeter = new AnimationTimer() {
      private final long[] frameTimes = new long[100];
      private int frameTimeIndex = 0;
      private boolean arrayFilled = false;

      @Override
      public void handle(long now) {
        long oldFrameTime = frameTimes[frameTimeIndex];

        frameTimes[frameTimeIndex] = now;
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length;

        if(frameTimeIndex == 0) {
          arrayFilled = true;
        }

        if(arrayFilled) {
          long elapsedNanos = now - oldFrameTime;
          long elapsedNanosPerFrame = elapsedNanos / frameTimes.length;
          double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame;
          double min = Double.MAX_VALUE;
          double max = 0;

          for(int i = 0; i < frameTimes.length - 1; i++) {
            long d = frameTimes[(i + frameTimeIndex + 1) % frameTimes.length] - frameTimes[(i + frameTimeIndex) % frameTimes.length];

            min = Math.min(min, d);
            max = Math.max(max, d);
          }

          fpsLabel.setText(String.format(" %.1f/%.1f ms - %.1f ", min / 1000000.0, max / 1000000.0, frameRate));
        }
      }
    };

    frameRateMeter.start();
*/
    return new StackPane(viewPort, clockPane, logoPane); //, fpsPane);
  }

  private static Node createLogo() {
    return new HBox() {{
      getStyleClass().addAll("program-name", "element");
      getChildren().add(new Label("Media") {{
        getStyleClass().add("left");
      }});
      getChildren().add(new Label("S") {{
        getStyleClass().add("center");
      }});
      getChildren().add(new Label("ystem") {{
        getStyleClass().add("right");
      }});
      setEffect(SpecialEffects.createNeonEffect(12));
      setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }};
  }

  private Node createViewPort(RootPresentation presentation) {
    ViewPort viewPort = viewPortFactory.create(presentation, n -> {

      /*
       * Special functionality for a background node:
       */

      Object backgroundNode = n.getProperties().get("background");

      if(backgroundNode != null) {
        sceneManager.setPlayerRoot(backgroundNode);
        sceneManager.fillProperty().set(Color.TRANSPARENT);

        presentation.clockVisible.set(false);
      }
      else {
        sceneManager.disposePlayerRoot();
        sceneManager.fillProperty().set(Color.BLACK);

        presentation.clockVisible.set(true);
      }
    });

    return viewPort;
  }
}

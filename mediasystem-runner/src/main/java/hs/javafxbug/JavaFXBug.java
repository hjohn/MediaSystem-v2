package hs.javafxbug;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class JavaFXBug extends Application {

  public static void main(String[] args) {
    Application.launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    MediaStatusIndicatorPane indicator = new MediaStatusIndicatorPane();

    primaryStage.setScene(new Scene(indicator));
    primaryStage.show();

    Timeline tl = new Timeline(
      new KeyFrame(Duration.ZERO, new KeyValue(indicator.value, 0.1)),
      new KeyFrame(Duration.seconds(2), new KeyValue(indicator.value, 0.99))
    );

    tl.setAutoReverse(true);
    tl.setCycleCount(Timeline.INDEFINITE);
    tl.play();

    // Background thread which frequently checks bounds of the arc group,
    // and stops the animation if the bounds are wrong.  Bounds height
    // should never exceed 50 (but it often does so slightly).  Sometimes
    // however it exceeds it by multiple pixels, causing jumps...
    //
    // The Width of the bounds also has this problem.
    //
    Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
      () -> Platform.runLater(() -> {
        Bounds boundsInLocal = indicator.group.getBoundsInLocal();

        if(boundsInLocal.getHeight() > 54) {
          System.out.println(boundsInLocal);
          System.out.println("arc: " + indicator.arc);
          System.out.println("arc: " + indicator.arc.getBoundsInLocal());
          tl.stop();
        }
      }),
      0,
      37,
      TimeUnit.MILLISECONDS
    );
  }

}

class MediaStatusIndicatorPane extends StackPane {
  public final DoubleProperty value = new SimpleDoubleProperty(0.0);

  public Group group;
  public Arc arc;

  public MediaStatusIndicatorPane() {
    value.addListener(obs -> {
      double percentage = value.get();

      List<Node> children = new ArrayList<>();
      Label bg = new Label();

      bg.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-scale-x: 1.1; -fx-scale-y: 1.1; -fx-min-width: 50; -fx-min-height: 50;");

      children.add(bg);

      arc = new Arc(0, 0, 22, 22, 90, -360 * percentage);

      arc.setStyle("-fx-stroke-width: 5; -fx-fill: transparent; -fx-stroke: rgba(125, 255, 125, 0.5)");
      arc.setType(ArcType.OPEN);

      Rectangle rectangle = new Rectangle(-25, -25, 50, 50);

      rectangle.setFill(new Color(1, 0, 0, 0.3));

      Label arcGraphic = new Label();

      group = new Group(rectangle, arc);
      arcGraphic.setGraphic(group);
      arcGraphic.setAlignment(Pos.TOP_CENTER);
      arcGraphic.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

      children.add(arcGraphic);

      getChildren().setAll(children);
    });
  }
}
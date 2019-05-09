package hs.javafxbug;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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

    indicator.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

    primaryStage.setScene(new Scene(indicator));
    primaryStage.show();

    Timeline tl = new Timeline(
      new KeyFrame(Duration.ZERO, new KeyValue(indicator.value, 0.1)),
      new KeyFrame(Duration.seconds(2), new KeyValue(indicator.value, 0.99))
    );

    tl.setAutoReverse(true);
    tl.setCycleCount(Timeline.INDEFINITE);
    tl.play();
  }

}

class MediaStatusIndicatorPane extends StackPane {
  public final DoubleProperty value = new SimpleDoubleProperty(0.0);

  public MediaStatusIndicatorPane() {
    value.addListener(obs -> {
      double percentage = value.get();

      List<Node> children = new ArrayList<>();

      children.add(new Label() {{ getStyleClass().add("indicator-pane-background"); }} );

      Arc circle = new Arc(0, 0, 22, 22, 90, 360 * (1 - percentage));

      circle.getStyleClass().add("indicator-circle-shape");

      Arc arc = new Arc(0, 0, 22, 22, 90, -360 * percentage);

      arc.setType(ArcType.OPEN);
      arc.getStyleClass().add("indicator-arc-shape");

      Rectangle rectangle = new Rectangle(-25, -25, 50, 50);

      rectangle.setFill(Color.TRANSPARENT);

      Label arcGraphic = new Label();

      arcGraphic.getStyleClass().add("indicator-arc");
      arcGraphic.setGraphic(new Group(rectangle, arc, circle));
      arcGraphic.setAlignment(Pos.TOP_CENTER);
      arcGraphic.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

      children.add(arcGraphic);
      children.add(new Label(String.format("%d%%", (int)(percentage * 100))) {{ getStyleClass().add("indicator-percentage"); }});

      getChildren().setAll(children);
    });
  }
}
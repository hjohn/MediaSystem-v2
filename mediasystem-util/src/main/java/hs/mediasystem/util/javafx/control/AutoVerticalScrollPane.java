package hs.mediasystem.util.javafx.control;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AutoVerticalScrollPane extends ScrollPane {
  private final int pixelsPerSecond;
  private final InvalidationListener listener;
  private final int fadePadding;

  private final DoubleProperty topFadePercentage = new SimpleDoubleProperty(0);
  private final DoubleProperty bottomFadePercentage = new SimpleDoubleProperty(0);

  private Timeline timeline;

  public AutoVerticalScrollPane(Node content, int pixelAreaPerSecond, int fadePadding) {
    super(new HBox(content));

    this.pixelsPerSecond = pixelAreaPerSecond;
    this.fadePadding = fadePadding;

    getStyleClass().setAll("auto-vertical-scroll-pane");
    setFitToWidth(true);
    setMouseTransparent(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    setVbarPolicy(ScrollBarPolicy.NEVER);

    this.listener = obs -> createTimeline(((Region)getContent()).getHeight());

    topFadePercentage.addListener(obs -> updateClip());
    bottomFadePercentage.addListener(obs -> updateClip());

    layoutBoundsProperty().addListener(listener);
    ((Region)getContent()).layoutBoundsProperty().addListener(listener);
    sceneProperty().addListener(listener);

    topFadePercentage.addListener(e -> updateClip());
    bottomFadePercentage.addListener(e -> updateClip());
  }

  private void createTimeline(double contentHeight) {
    if(timeline != null) {
      timeline.stop();
    }

    timeline = null;

    if(getScene() != null) {
      double invisibleArea = (contentHeight - getHeight()) * getWidth();

      if(invisibleArea > 0) {
        int scrollSeconds = (int)(invisibleArea / pixelsPerSecond);

        timeline = new Timeline(
          30,
          new KeyFrame(Duration.ZERO, new KeyValue(vvalueProperty(), 0), new KeyValue(topFadePercentage, 0), new KeyValue(bottomFadePercentage, 1)),
          new KeyFrame(Duration.seconds(9), new KeyValue(topFadePercentage, 0)),
          new KeyFrame(Duration.seconds(10), new KeyValue(vvalueProperty(), 0), new KeyValue(topFadePercentage, 1)),
          new KeyFrame(Duration.seconds(10 + scrollSeconds), new KeyValue(vvalueProperty(), 1), new KeyValue(bottomFadePercentage, 1)),
          new KeyFrame(Duration.seconds(10 + scrollSeconds + 1), new KeyValue(bottomFadePercentage, 0)),
          new KeyFrame(Duration.seconds(10 + scrollSeconds + 19), new KeyValue(bottomFadePercentage, 0)),
          new KeyFrame(Duration.seconds(10 + scrollSeconds + 20), new KeyValue(vvalueProperty(), 1), new KeyValue(bottomFadePercentage, 1)),
          new KeyFrame(Duration.seconds(10 + scrollSeconds + 20 + 1), new KeyValue(vvalueProperty(), 0), new KeyValue(topFadePercentage, 1)),
          new KeyFrame(Duration.seconds(10 + scrollSeconds + 20 + 2), new KeyValue(topFadePercentage, 0))
        );

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
      }
      else {
        bottomFadePercentage.set(0);
        topFadePercentage.set(0);
      }
    }
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();

    updateClip();
  }

  private void updateClip() {
    Rectangle rectangle = new Rectangle(0, 0, getWidth(), getHeight());
    double offset = fadePadding / getHeight();

    rectangle.setFill(new LinearGradient(0, 0, 0, getHeight(), false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.TRANSPARENT),
      new Stop(offset * topFadePercentage.get(), Color.BLACK),
      new Stop(1 - offset * bottomFadePercentage.get(), Color.BLACK),
      new Stop(1.0, Color.TRANSPARENT)
    ));

    setClip(rectangle);
  }

  @Override
  public Orientation getContentBias() {
    return getContent().getContentBias();
  }
}

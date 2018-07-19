package hs.mediasystem.util.javafx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AutoVerticalScrollPane extends ScrollPane {
  private final int pixelsPerSecond;
  private final ChangeListener<? super Number> listener;
  private final int fadePadding;

  private Timeline timeline;

  public AutoVerticalScrollPane(Node content, int pixelAreaPerSecond, int fadePadding) {
    super(new HBox(content));

    this.pixelsPerSecond = pixelAreaPerSecond;
    this.fadePadding = fadePadding;

    ((Pane)getContent()).setPadding(new Insets(fadePadding, 0, fadePadding, 0));

    getStyleClass().setAll("auto-vertical-scroll-pane");
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    setVbarPolicy(ScrollBarPolicy.NEVER);

    this.listener = (obs, old, current) -> createTimeline(((Region)getContent()).getHeight());

    ((Region)getContent()).heightProperty().addListener(listener);
    heightProperty().addListener(listener);
  }

  private void createTimeline(double contentHeight) {
    if(timeline != null) {
      timeline.stop();
    }

    timeline = null;

    double invisibleArea = (contentHeight - getHeight()) * getWidth();

    if(invisibleArea > 0) {
      int scrollSeconds = (int)(invisibleArea / pixelsPerSecond);

      timeline = new Timeline(
        30,
        new KeyFrame(Duration.ZERO, new KeyValue(vvalueProperty(), 0)),
        new KeyFrame(Duration.seconds(10), new KeyValue(vvalueProperty(), 0)),
        new KeyFrame(Duration.seconds(10 + scrollSeconds), new KeyValue(vvalueProperty(), 1)),
        new KeyFrame(Duration.seconds(10 + scrollSeconds + 20), new KeyValue(vvalueProperty(), 1)),
        new KeyFrame(Duration.seconds(10 + scrollSeconds + 20 + 1), new KeyValue(vvalueProperty(), 0))
      );

      timeline.setCycleCount(Animation.INDEFINITE);
      timeline.play();
    }
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();

    Rectangle rectangle = new Rectangle(0, 0, getWidth(), getHeight());
    double offset = fadePadding / getHeight();

    rectangle.setFill(new LinearGradient(0, 0, 0, getHeight(), false, CycleMethod.NO_CYCLE,
      new Stop(0, Color.TRANSPARENT),
      new Stop(offset, Color.BLACK),
      new Stop(1 - offset, Color.BLACK),
      new Stop(1.0, Color.TRANSPARENT)
    ));

    setClip(rectangle);
  }

  @Override
  public Orientation getContentBias() {
    return getContent().getContentBias();
  }
}

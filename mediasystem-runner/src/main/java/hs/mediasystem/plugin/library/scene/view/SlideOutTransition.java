package hs.mediasystem.plugin.library.scene.view;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class SlideOutTransition extends Transition {
  private final Node node;
  private final Pane pane;
  private final double startX;

  public SlideOutTransition(Pane pane, Node node) {
    this.pane = pane;
    this.node = node;
    this.startX = node.getTranslateX();

    setCycleDuration(Duration.millis(500));
    setInterpolator(Interpolator.EASE_IN);
  }

  @Override
  protected void interpolate(double frac) {
    node.setTranslateX((pane.getWidth() + 100) * frac + startX);
  }
}

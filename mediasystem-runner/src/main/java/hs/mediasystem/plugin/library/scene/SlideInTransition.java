package hs.mediasystem.plugin.library.scene;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class SlideInTransition extends Transition {
  private final Node node;
  private final Pane pane;

  public SlideInTransition(Pane pane, Node node) {
    this.pane = pane;
    this.node = node;

    setCycleDuration(Duration.millis(2500));
    setInterpolator(Interpolator.EASE_OUT);
  }

  @Override
  protected void interpolate(double frac) {
    if(pane.getWidth() == 0) {
      return;
    }

    if(frac < 0.8) {  // TODO checking fraction like this doesn't play well with interpolator
      node.setTranslateX(pane.getWidth() + 100);
    }
    else {
      node.setVisible(true);
      node.setManaged(true);
      node.setTranslateX((pane.getWidth() + 100) * (1 - frac) * 5);
    }
  }
}
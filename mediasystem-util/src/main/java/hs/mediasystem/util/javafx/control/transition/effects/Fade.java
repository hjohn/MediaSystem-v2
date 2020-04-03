package hs.mediasystem.util.javafx.control.transition.effects;

import hs.mediasystem.util.javafx.control.transition.TransitionEffect;

import javafx.animation.Interpolator;
import javafx.scene.Node;

/**
 * Fades a {@link Node} from fully transparent to its original opacity level.
 */
public class Fade implements TransitionEffect {
  private final Interpolator interpolator;

  public Fade(Interpolator interpolator) {
    this.interpolator = interpolator;
  }

  public Fade() {
    this(Interpolator.LINEAR);
  }

  @Override
  public Interpolator getInterpolator() {
    return interpolator;
  }

  @Override
  public Interpolatable create(Node node, boolean invert) {
    double startOpacity = node.getOpacity();

    return new Interpolatable() {
      @Override
      public void apply(double frac) {
        node.setOpacity(startOpacity * frac);
      }
    };
  }
}

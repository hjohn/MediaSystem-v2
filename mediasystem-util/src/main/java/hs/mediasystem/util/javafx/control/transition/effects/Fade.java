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
    return new InternalInterpolatable(node, node.getOpacity(), 0);
  }

  private final class InternalInterpolatable implements Interpolatable {
    final Node node;
    final double original;
    final double base;

    InternalInterpolatable(Node node, double original, double base) {
      this.node = node;
      this.original = original;
      this.base = base;
    }

    @Override
    public void apply(double frac) {
      node.setOpacity(base + (original - base) * frac);
    }

    @Override
    public Interpolatable derive() {
      return new InternalInterpolatable(node, original, node.getOpacity());
    }
  }
}

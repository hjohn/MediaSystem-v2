package hs.mediasystem.util.javafx.ui.transition.domain;

import javafx.animation.Interpolator;
import javafx.scene.Node;

/**
 * Interface representing a transition effect that can be applied to a {@link Node}.
 */
public interface TransitionEffect {

  /**
   * Interface which can be used to apply a transition effect.
   */
  public interface Interpolatable {

    /**
     * Applies the transition effect at the given level, where 0 represents the beginning of the
     * effect and 1 the end of the effect.
     *
     * @param frac a fractional value between 0 and 1, with 0 representing the start of the effect and 1 the end
     */
    void apply(double frac);

    /**
     * Derives a new {@link Interpolatable} based on this one, which starts its interpolation
     * from the current value(s) of the original {@link Node}.
     *
     * @return a new {@link Interpolatable}, never null
     */
    Interpolatable derive();
  }

  /**
   * Returns the desired {@link Interpolator} with which to convert the
   * fractional animation position.
   *
   * @return an {@link Interpolator}, cannot be null
   */
  Interpolator getInterpolator();

  /**
   * Creates an {@link Interpolatable} which can be used to animate this
   * transition effect over a period of time.
   *
   * @param node a {@link Node} to animate, cannot be null
   * @param invert whether the animation direction should be inverted (only applies to animations that have a sense of direction)
   * @return an {@link Interpolatable} which can be used to animate the node, never null
   */
  Interpolatable create(Node node, boolean invert);
}
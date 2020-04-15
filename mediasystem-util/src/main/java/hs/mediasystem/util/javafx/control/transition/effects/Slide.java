package hs.mediasystem.util.javafx.control.transition.effects;

import hs.mediasystem.util.javafx.control.transition.TransitionEffect;

import javafx.animation.Interpolator;
import javafx.scene.Node;

/**
 * Slides a {@link Node} in or out from/to the given direction.
 */
public class Slide implements TransitionEffect {
  public enum Direction {
    LEFT(false),
    RIGHT(false),
    UP(true),
    DOWN(true);

    private boolean vertical;

    Direction(boolean vertical) {
      this.vertical = vertical;
    }

    public boolean isVertical() {
      return vertical;
    }

    public Direction opposite() {
      switch(this) {
      case UP: return DOWN;
      case DOWN: return UP;
      case LEFT: return RIGHT;
      case RIGHT: return LEFT;
      default: throw new IllegalStateException();
      }
    }
  }

  private final Interpolator interpolator;
  private final Direction direction;

  public Slide(Interpolator interpolator, Direction direction) {
    this.interpolator = interpolator;
    this.direction = direction;
  }

  @Override
  public Interpolator getInterpolator() {
    return interpolator;
  }

  @Override
  public Interpolatable create(Node node, boolean invert) {
    double original = direction.isVertical() ? node.getTranslateY() : node.getTranslateX();
    double sign = (direction == Direction.RIGHT || direction == Direction.DOWN) != invert ? 1 : -1;

    return new InternalInterpolatable(direction, node, original, sign);
  }

  private static final class InternalInterpolatable implements Interpolatable {
    final Direction direction;
    final Node node;
    final double original;
    final double sign;

    InternalInterpolatable(Direction direction, Node node, double original, double sign) {
      this.direction = direction;
      this.node = node;
      this.original = original;
      this.sign = sign;
    }

    @Override
    public void apply(double frac) {
      double base = sign * (direction.isVertical() ? node.getLayoutBounds().getHeight() : node.getLayoutBounds().getWidth());
      double value = base + (original - base) * frac;

      if(direction.isVertical()) {
        node.setTranslateY(value);
      }
      else {
        node.setTranslateX(value);
      }
    }

    @Override
    public Interpolatable derive() {
      return new InternalInterpolatable(direction, node, original, direction.isVertical() ? node.getTranslateY() : node.getTranslateX());
    }
  }
}
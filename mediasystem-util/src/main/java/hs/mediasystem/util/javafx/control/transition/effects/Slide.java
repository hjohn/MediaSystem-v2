package hs.mediasystem.util.javafx.control.transition.effects;

import hs.mediasystem.util.javafx.control.transition.TransitionEffect;

import javafx.animation.Interpolator;
import javafx.geometry.Bounds;
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
    Bounds layoutBounds = node.getParent().getLayoutBounds();
    double start = direction.isVertical() ? node.getTranslateY() : node.getTranslateX();
    double d = (direction == Direction.RIGHT || direction == Direction.DOWN) != invert ? 1 : -1;

    return new Interpolatable() {
      @Override
      public void apply(double frac) {
        if(direction.isVertical()) {
          node.setTranslateY(d * layoutBounds.getHeight() * (1 - frac) + start);
        }
        else {
          node.setTranslateX(d * layoutBounds.getWidth() * (1 - frac) + start);
        }
      }
    };
  }
}
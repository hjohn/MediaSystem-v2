package hs.mediasystem.util.javafx.control.transition.multi;

import hs.mediasystem.util.javafx.control.transition.EffectList;
import hs.mediasystem.util.javafx.control.transition.MultiNodeTransition;
import hs.mediasystem.util.javafx.control.transition.TransitionEffect;
import hs.mediasystem.util.javafx.control.transition.TransitionEffect.Interpolatable;

import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Combines multiple {@link EffectList}s (with {@link TransitionEffect}s) to
 * create a custom transition.
 */
public class Custom implements MultiNodeTransition {
  enum Action {
    SHOW, REMOVE
  }

  private static final String ACTION = Custom.class.getName() + ":action";
  private static final String TIMELINE = Custom.class.getName() + ":timeline";

  private final Duration initialDelay;
  private final EffectList intro;
  private final EffectList outro;

  public Custom(Duration initialDelay, EffectList intro, EffectList outro) {
    this.initialDelay = initialDelay;
    this.intro = intro;
    this.outro = outro;
  }

  public Custom(EffectList intro, EffectList outro) {
    this(Duration.ZERO, intro, outro);
  }

  public Custom(Duration initialDelay, EffectList effects) {
    this(initialDelay, effects, effects);
  }

  public Custom(EffectList effects) {
    this(Duration.ZERO, effects, effects);
  }

  @Override
  public void restart(List<? extends Node> children, boolean invert) {

    /*
     * Lifecycle here:
     * - Node starts managed (it is always visible so it can receive focus)
     * - If a node is removed before initial delay is over, it is set to unmanaged (and will be removed)
     * - When outro animation ends, the node is set to unmanaged (and will be removed)
     *
     * Removal of unmanaged nodes is handled by the container.
     */

    for(int i = children.size() - 1; i >= 0; i--) {
      Node node = children.get(i);
      Action previousAction = (Action)node.getProperties().get(ACTION);

      if(previousAction == null) {
        node.getProperties().put(ACTION, Action.SHOW);

        Transition transition = create(intro, node, true, invert);
        Timeline timeline = new Timeline(new KeyFrame(initialDelay, e -> transition.play()), new KeyFrame(initialDelay.add(Duration.millis(1))));

        node.getProperties().put(TIMELINE, timeline);

        timeline.play();
      }
      else if(previousAction == Action.SHOW) {
        Timeline timeline = (Timeline)node.getProperties().get(TIMELINE);

        if(timeline.getStatus() == Status.RUNNING) {  // early removal, if initial delay has not yet elapsed
          timeline.stop();
          node.setManaged(false);
          continue;
        }

        node.getProperties().put(ACTION, Action.REMOVE);

        Transition transition = create(outro, node, false, invert);

        transition.setOnFinished(e -> node.setManaged(false));
        transition.play();
      }
    }
  }

  private class Wrapper implements Interpolatable {
    final Interpolatable delegate;
    final Interpolator interpolator;

    Wrapper(Interpolatable delegate, Interpolator interpolator) {
      this.delegate = delegate;
      this.interpolator = interpolator;
    }

    @Override
    public void apply(double frac) {
      delegate.apply(frac);
    }
  }

  public Transition create(EffectList list, Node node, boolean intro, boolean invert) {
    List<Wrapper> interpolatables = list.getEffects().stream()
      .map(e -> new Wrapper(e.create(node, invert), e.getInterpolator()))
      .collect(Collectors.toList());

    if(intro) {
      // Run once at fraction 0 so nodes are correctly positioned/setup, despite initial delay
      for(Wrapper w : interpolatables) {
        w.apply(w.interpolator.interpolate(0.0, 1.0, 0.0));
      }
    }

    return new Transition() {
      {
        setCycleDuration(list.getDuration());
        setInterpolator(Interpolator.LINEAR);
      }

      @Override
      protected void interpolate(double frac) {
        for(Wrapper w : interpolatables) {
          w.apply(w.interpolator.interpolate(intro ? 0.0 : 1.0, intro ? 1.0 : 0.0, frac));
        }
      }
    };
  }
}

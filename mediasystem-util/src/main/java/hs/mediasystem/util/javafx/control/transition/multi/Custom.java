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
    SHOW, REMOVE, FINISHED
  }

  private static final String STATE = Custom.class.getName() + ":state";

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
  public void restart(List<? extends Node> children, Node targetNode, boolean invert) {

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

      State state = (State)node.getProperties().computeIfAbsent(STATE, k -> new State(create(intro, node, true, invert)));

      if(state.action == Action.SHOW) {
        state.action = Action.REMOVE;
        state.timeline = new Timeline(new KeyFrame(initialDelay, e -> state.originalIntro.play()));

        if(initialDelay.equals(Duration.ZERO)) {
          state.originalIntro.play();  // Timelines of Duration.ZERO length donot trigger their event action, so trigger it immediately here instead
        }
        else {
          state.timeline.play();
        }
      }
      else if(state.action == Action.REMOVE) {
        if(state.timeline.getStatus() == Status.RUNNING) {  // early removal, if initial delay has not yet elapsed
          removeNode(node);
          continue;
        }

        state.action = Action.FINISHED;
        state.intro.stop();
        state.outro = create(outro, node, false, invert);  // created on demand so it starts from correct start values
        state.outro.play();
      }
      else if(state.action == Action.FINISHED && node.equals(targetNode)) {  // node is to be shown again (due to re-add)

        /*
         * This occurs when a node that was in the process of being removed
         * gets re-added before the removal completed.  The transition is
         * not recreated as it contains information about the initial state
         * of the animated properties.
         *
         * If initialDelay has not elapsed, no further action is taken apart
         * from cancelling the timeline.  If it has elapsed, the intro
         * transition is started.
         */

        state.action = Action.REMOVE;

        if(state.timeline.getStatus() == Status.RUNNING) {
          state.timeline.stop();
          continue;
        }

        state.outro.stop();
        state.intro = state.originalIntro.derive();
        state.intro.play();
      }
    }
  }

  private static void removeNode(Node node) {
    State state = (State)node.getProperties().remove(STATE);

    state.timeline.stop();
    state.originalIntro.interpolate(1.0);  // resets all interpolated values to their defaults

    node.setManaged(false);
  }

  private static class Wrapper implements Interpolatable {
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

    @Override
    public Wrapper derive() {
      return new Wrapper(delegate.derive(), interpolator);
    }
  }

  public PublicTransition create(EffectList list, Node node, boolean intro, boolean invert) {
    List<Wrapper> interpolatables = list.getEffects().stream()
      .map(e -> new Wrapper(e.create(node, invert), e.getInterpolator()))
      .collect(Collectors.toList());

    PublicTransition publicTransition = new PublicTransition(interpolatables, list.getDuration(), intro);

    if(intro) {
      // Run once at fraction 0 so nodes are correctly positioned/setup, despite initial delay
      publicTransition.interpolate(0);
    }
    else {
      publicTransition.setOnFinished(e -> removeNode(node));
    }

    return publicTransition;
  }

  private static class State {
    final PublicTransition originalIntro;

    PublicTransition intro;
    PublicTransition outro;
    Action action = Action.SHOW;
    Timeline timeline;

    State(PublicTransition intro) {
      this.originalIntro = intro;
      this.intro = intro;
    }
  }

  private static class PublicTransition extends Transition {
    final List<Wrapper> interpolatables;
    final boolean intro;

    PublicTransition(List<Wrapper> interpolatables, Duration duration, boolean intro) {
      this.intro = intro;

      setCycleDuration(duration);
      setInterpolator(Interpolator.LINEAR);

      this.interpolatables = interpolatables;
    }

    @Override
    public void interpolate(double frac) {
      for(Wrapper w : interpolatables) {
        w.apply(w.interpolator.interpolate(intro ? 0.0 : 1.0, intro ? 1.0 : 0.0, frac));
      }
    }

    public PublicTransition derive() {
      return new PublicTransition(
        interpolatables.stream().map(Wrapper::derive).collect(Collectors.toList()),
        getCycleDuration(),
        intro
      );
    }
  }
}

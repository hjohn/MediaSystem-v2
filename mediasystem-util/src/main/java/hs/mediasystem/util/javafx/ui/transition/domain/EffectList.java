package hs.mediasystem.util.javafx.ui.transition.domain;

import java.util.List;

import javafx.util.Duration;

/**
 * A list of effects and their duration.
 */
public class EffectList {
  private final List<TransitionEffect> effects;
  private final Duration duration;

  public EffectList(Duration duration, List<TransitionEffect> effects) {
    this.duration = duration;
    this.effects = effects;
  }

  public EffectList(Duration duration, TransitionEffect effect) {
    this(duration, List.of(effect));
  }

  public List<TransitionEffect> getEffects() {
    return effects;
  }

  public Duration getDuration() {
    return duration;
  }
}
package hs.mediasystem.util.javafx.control.transition;

import hs.mediasystem.util.javafx.control.transition.effects.Fade;
import hs.mediasystem.util.javafx.control.transition.multi.Custom;

import javafx.util.Duration;

public class StandardTransitions {

  public static final MultiNodeTransition fade(int millis, int delayMillis) {
    return new Custom(Duration.millis(delayMillis), new EffectList(Duration.millis(millis), new Fade()));
  }

  public static final MultiNodeTransition fade(int millis) {
    return new Custom(new EffectList(Duration.millis(millis), new Fade()));
  }

  public static final MultiNodeTransition fade() {
    return fade(500);
  }
}

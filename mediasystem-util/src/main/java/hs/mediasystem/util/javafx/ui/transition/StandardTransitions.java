package hs.mediasystem.util.javafx.ui.transition;

import hs.mediasystem.util.javafx.ui.transition.domain.EffectList;
import hs.mediasystem.util.javafx.ui.transition.domain.MultiNodeTransition;
import hs.mediasystem.util.javafx.ui.transition.effects.Fade;
import hs.mediasystem.util.javafx.ui.transition.multi.Custom;

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

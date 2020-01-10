package hs.mediasystem.ui.api.domain;

import java.time.Duration;
import java.time.Instant;

import org.reactfx.value.Var;

public class State {
  private final Var<Instant> lastConsumptionTime;
  private final Var<Boolean> consumed;
  private final Var<Duration> resumePosition;

  public State(Var<Instant> lastWatchedTime, Var<Boolean> consumed, Var<Duration> resumePosition) {
    this.lastConsumptionTime = lastWatchedTime;
    this.consumed = consumed;
    this.resumePosition = resumePosition;
  }

  public Var<Instant> getLastConsumptionTime() {
    return lastConsumptionTime;
  }

  public Var<Boolean> isConsumed() {
    return consumed;
  }

  public Var<Duration> getResumePosition() {
    return resumePosition;
  }
}

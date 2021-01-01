package hs.mediasystem.ui.api.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class State {
  public static final State EMPTY = new State(null, false, Duration.ZERO);

  private final Optional<Instant> lastConsumptionTime;
  private final boolean consumed;
  private final Duration resumePosition;

  public State(Instant lastWatchedTime, boolean consumed, Duration resumePosition) {
    if(resumePosition == null) {
      throw new IllegalArgumentException("resumePosition cannot be null");
    }

    this.lastConsumptionTime = Optional.ofNullable(lastWatchedTime);
    this.consumed = consumed;
    this.resumePosition = resumePosition;
  }

  public Optional<Instant> getLastConsumptionTime() {
    return lastConsumptionTime;
  }

  public boolean isConsumed() {
    return consumed;
  }

  public Duration getResumePosition() {
    return resumePosition;
  }
}

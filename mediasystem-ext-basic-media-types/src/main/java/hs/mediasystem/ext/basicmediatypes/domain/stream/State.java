package hs.mediasystem.ext.basicmediatypes.domain.stream;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class State {
  private final Optional<Instant> lastConsumptionTime;
  private final boolean consumed;
  private final Duration resumePosition;

  public State(Instant lastWatchedTime, boolean watched, Duration resumePosition) {
    if(resumePosition == null || resumePosition.isNegative()) {
      throw new IllegalArgumentException("resumePosition cannot be null or negative: " + resumePosition);
    }

    this.lastConsumptionTime = Optional.ofNullable(lastWatchedTime);
    this.consumed = watched;
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

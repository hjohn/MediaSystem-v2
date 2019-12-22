package hs.mediasystem.ext.basicmediatypes.domain.stream;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class State {
  private final Optional<Instant> lastWatchedTime;
  private final boolean watched;
  private final Duration resumePosition;

  public State(Instant lastWatchedTime, boolean watched, Duration resumePosition) {
    if(resumePosition == null || resumePosition.isNegative()) {
      throw new IllegalArgumentException("resumePosition cannot be null or negative: " + resumePosition);
    }

    this.lastWatchedTime = Optional.ofNullable(lastWatchedTime);
    this.watched = watched;
    this.resumePosition = resumePosition;
  }

  public Optional<Instant> getLastWatchedTime() {
    return lastWatchedTime;
  }

  public boolean isWatched() {
    return watched;
  }

  public Duration getResumePosition() {
    return resumePosition;
  }
}

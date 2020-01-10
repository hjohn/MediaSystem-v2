package hs.mediasystem.ui.api.domain;

import java.time.Duration;
import java.util.Optional;

public class Recommendation {
  private final Work work;
  private final boolean isWatched;
  private final Optional<Duration> length;
  private final Duration position;

  public Recommendation(Work work, boolean isWatched, Duration length, Duration position) {
    this.work = work;
    this.isWatched = isWatched;
    this.length = Optional.ofNullable(length);
    this.position = position;
  }

  public boolean isWatched() {
    return isWatched;
  }


  public Optional<Duration> getLength() {
    return length;
  }

  public Duration getPosition() {
    return position;
  }

  public Work getWork() {
    return work;
  }
}

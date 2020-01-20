package hs.mediasystem.ui.api.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class Recommendation {
  private final Work work;
  private final boolean isWatched;
  private final Optional<Duration> length;
  private final Duration position;
  private final Instant lastTimeWatched;

  public Recommendation(Work work, Instant lastTimeWatched, boolean isWatched, Duration length, Duration position) {
    this.work = work;
    this.lastTimeWatched = lastTimeWatched;
    this.isWatched = isWatched;
    this.length = Optional.ofNullable(length);
    this.position = position;
  }

  public boolean isWatched() {
    return isWatched;
  }

  /**
   * The time when the item this recommendation is based on was watched last.
   *
   * @return an {@link Instant}, never <code>null</code>
   */
  public Instant getLastTimeWatched() {
    return lastTimeWatched;
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

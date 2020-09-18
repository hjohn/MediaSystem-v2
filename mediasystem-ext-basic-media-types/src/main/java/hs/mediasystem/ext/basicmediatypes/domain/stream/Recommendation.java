package hs.mediasystem.ext.basicmediatypes.domain.stream;

import java.time.Instant;

/**
 * Recommends a {@link Work} based on another work or (a specific stream of) itself.
 */
public class Recommendation {
  private final Instant instant;
  private final Work work;

  /**
   * Constructs a new instance.
   *
   * @param instant an {@link Instant} indicating the relevant time for this recommendation, cannot be null
   * @param work a {@link Work} that is recommended, cannot be null
   */
  public Recommendation(Instant instant, Work work) {
    if(instant == null) {
      throw new IllegalArgumentException("instant cannot be null");
    }
    if(work == null) {
      throw new IllegalArgumentException("work cannot be null");
    }

    this.instant = instant;
    this.work = work;
  }

  /**
   * Returns an {@link Instant} indicating the relevant time for this recommendation.
   *
   * @return an {@link Instant} indicating the relevant time for this recommendation, never null
   */
  public Instant getInstant() {
    return instant;
  }

  /**
   * Returns a {@link Work} that is recommended.
   *
   * @return a {@link Work} that is recommended, never null
   */
  public Work getWork() {
    return work;
  }
}

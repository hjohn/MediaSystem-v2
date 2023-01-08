package hs.mediasystem.api.datasource.domain.stream;

import java.time.Instant;

/**
 * Recommends a {@link Work} based on another work or (a specific stream of) itself.
 *
 * @param instant an {@link Instant} indicating the relevant time for this recommendation, cannot be null
 * @param work a {@link Work} that is recommended, cannot be null
 */
public record Recommendation(Instant instant, Work work) {
  public Recommendation {
    if(instant == null) {
      throw new IllegalArgumentException("instant cannot be null");
    }
    if(work == null) {
      throw new IllegalArgumentException("work cannot be null");
    }
  }
}

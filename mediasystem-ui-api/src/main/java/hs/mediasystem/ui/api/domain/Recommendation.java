package hs.mediasystem.ui.api.domain;

import java.time.Instant;

/**
 * Represents a Work that is recommended based on some criteria.<p>
 *
 * The recommended work may be recommended based on another work.  Therefore
 * the sample time may not match the last time watched.
 */
public class Recommendation {
  private final Work work;
  private final Instant sampleTime;

  public Recommendation(Work work, Instant sampleTime) {
    if(work == null) {
      throw new IllegalArgumentException("work cannot be null");
    }
    if(sampleTime == null) {
      throw new IllegalArgumentException("sampleTime cannot be null");
    }

    this.work = work;
    this.sampleTime = sampleTime;
  }

  /**
   * Returns the recommended {@link Work}.
   *
   * @return the recommended {@link Work}, never <code>null</code>
   */
  public Work getWork() {
    return work;
  }

  /**
   * The relevant time for the recommendation.  In case of a partially watched
   * recommendation, this is the time it was last watched.  In case of a next
   * in sequence recommendation, this is the time the previous item in the sequence
   * was watched and in case of a new recommendation, this is the time when the
   * new item was discovered.
   *
   * @return an {@link Instant}, never <code>null</code>
   */
  public Instant getSampleTime() {
    return sampleTime;
  }
}

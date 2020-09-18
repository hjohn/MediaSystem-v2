package hs.mediasystem.domain.work;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents the state of a Streamable.  The watched state is independent
 * of the resume position (a watched worked can be watched again).
 */
public class State {
  private final Optional<Instant> lastConsumptionTime;
  private final boolean consumed;
  private final Duration resumePosition;

  /**
   * Constructs a new instance.
   *
   * @param lastConsumptionTime a time when last consumed, can be null
   * @param consumed whether consumed or not
   * @param resumePosition a resume position, cannot be null or negative
   */
  public State(Instant lastConsumptionTime, boolean consumed, Duration resumePosition) {
    if(resumePosition == null || resumePosition.isNegative()) {
      throw new IllegalArgumentException("resumePosition cannot be null or negative: " + resumePosition);
    }

    this.lastConsumptionTime = Optional.ofNullable(lastConsumptionTime);
    this.consumed = consumed;
    this.resumePosition = resumePosition;
  }

  /**
   * Returns the last consumption time.
   *
   * @return the last consumption time
   */
  public Optional<Instant> getLastConsumptionTime() {
    return lastConsumptionTime;
  }

  /**
   * Returns consumption status.
   *
   * @return <code>true</code> if consumed, otherwise <code>false</code>
   */
  public boolean isConsumed() {
    return consumed;
  }

  /**
   * Returns the resume position.
   *
   * @return the resume position, never null and never negative
   */
  public Duration getResumePosition() {
    return resumePosition;
  }
}

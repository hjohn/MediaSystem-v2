package hs.mediasystem.domain.work;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents the state of a Streamable.  The watched state is independent
 * of the resume position (a watched worked can be watched again).
 *
 * @param lastConsumptionTime the last consumption time
 * @param consumed <code>true</code> if consumed, otherwise <code>false</code>
 * @param resumePosition the resume position, never {@code null} and never negative
 */
public record State(Optional<Instant> lastConsumptionTime, boolean consumed, Duration resumePosition) {
  public State {
    if(resumePosition == null || resumePosition.isNegative()) {
      throw new IllegalArgumentException("resumePosition cannot be null or negative: " + resumePosition);
    }
  }
}

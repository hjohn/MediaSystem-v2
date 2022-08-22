package hs.mediasystem.ui.api.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

public record State(Optional<Instant> lastConsumptionTime, boolean consumed, Duration resumePosition) {
  public static final Comparator<State> WATCHED_DATE_REVERSED = Comparator.comparing((State s) -> s.lastConsumptionTime().orElse(null), Comparator.nullsLast(Comparator.naturalOrder())).reversed();
  public static final State EMPTY = new State(null, false, Duration.ZERO);

  public State {
    if(resumePosition == null) {
      throw new IllegalArgumentException("resumePosition cannot be null");
    }
  }
}

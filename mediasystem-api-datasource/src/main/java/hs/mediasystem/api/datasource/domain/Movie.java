package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

import java.time.Duration;
import java.util.Set;

public class Movie extends Production {
  public enum State {
    PLANNED,
    IN_PRODUCTION,
    RELEASED
  }

  private final Duration runtime;
  private final State state;

  public Movie(WorkId id, Details details, String tagLine, Reception reception, Duration runtime, Classification classification, double popularity, State state, Set<WorkId> relatedWorks) {
    super(id, details, tagLine, reception, classification, popularity, relatedWorks);

    this.runtime = runtime;
    this.state = state;
  }

  public Duration getRuntime() {
    return runtime;
  }

  public State getState() {
    return state;
  }
}

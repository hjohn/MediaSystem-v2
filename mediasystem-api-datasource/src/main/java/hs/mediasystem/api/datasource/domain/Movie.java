package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Context;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

import java.time.Duration;

public class Movie extends Production {
  public enum State {
    PLANNED,
    IN_PRODUCTION,
    RELEASED
  }

  private final Duration runtime;
  private final State state;

  public Movie(WorkId id, Details details, Reception reception, Context context, String tagLine, Duration runtime, Classification classification, double popularity, State state) {
    super(id, details, reception, context, tagLine, classification, popularity);

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

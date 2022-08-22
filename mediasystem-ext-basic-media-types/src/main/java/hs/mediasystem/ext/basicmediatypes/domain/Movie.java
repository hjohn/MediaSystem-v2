package hs.mediasystem.ext.basicmediatypes.domain;

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
  private final String tagLine;
  private final State state;

  public Movie(WorkId id, Details details, Reception reception, Duration runtime, Classification classification, double popularity, String tagLine, State state, Set<WorkId> relatedWorks) {
    super(id, details, reception, classification, popularity, relatedWorks);

    this.runtime = runtime;
    this.state = state;
    this.tagLine = tagLine;
  }

  public Duration getRuntime() {
    return runtime;
  }

  public State getState() {
    return state;
  }

  public String getTagLine() {
    return tagLine;
  }
}

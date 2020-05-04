package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.work.Reception;

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

  public Movie(ProductionIdentifier identifier, Details details, Reception reception, Duration runtime, Classification classification, double popularity, String tagLine, State state, Set<Identifier> relatedIdentifiers) {
    super(identifier, details, reception, classification, popularity, relatedIdentifiers);

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

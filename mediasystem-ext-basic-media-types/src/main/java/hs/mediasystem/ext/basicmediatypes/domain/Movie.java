package hs.mediasystem.ext.basicmediatypes.domain;

import java.time.Duration;
import java.util.List;

public class Movie extends Production {
  public enum State {
    PLANNED,
    IN_PRODUCTION,
    RELEASED
  }

  private final String tagLine;
  private final State state;
  private final double popularity;
  private final Chronology chronology;

  public Movie(ProductionIdentifier identifier, Details details, Reception reception, Duration runtime, List<String> languages, List<String> genres, double popularity, String tagLine, State state, Chronology chronology) {
    super(identifier, details, reception, runtime, languages, genres);

    this.popularity = popularity;
    this.state = state;
    this.tagLine = tagLine;
    this.chronology = chronology;
  }

  public State getState() {
    return state;
  }

  public String getTagLine() {
    return tagLine;
  }

  public double getPopularity() {
    return popularity;
  }

  public Chronology getChronology() {
    return chronology;
  }
}

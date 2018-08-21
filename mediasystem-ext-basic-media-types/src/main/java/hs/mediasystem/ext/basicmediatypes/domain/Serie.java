package hs.mediasystem.ext.basicmediatypes.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Serie extends Production {
  public enum State {
    ENDED,
    CONTINUING,
    PLANNED,
    IN_PRODUCTION,
    CANCELED,
    PILOT
  }

  private final State state;
  private final List<Season> seasons;
  private final LocalDate lastAirDate;
  private final double popularity;

  public Serie(ProductionIdentifier identifier, Details details, Reception reception, Duration runtime, List<String> languages, List<String> genres, State state, LocalDate lastAirDate, double popularity, List<Season> seasons) {
    super(identifier, details, reception, runtime, languages, genres);

    this.popularity = popularity;
    this.state = state;
    this.lastAirDate = lastAirDate;
    this.seasons = new ArrayList<>(Collections.unmodifiableList(seasons));
  }

  public State getState() {
    return state;
  }

  public LocalDate getLastAirDate() {
    return lastAirDate;
  }

  public List<Season> getSeasons() {
    return seasons;
  }

  public double getPopularity() {
    return popularity;
  }

  public Season findSeason(int seasonNumber) {
    return seasons.stream().filter(s -> s.getNumber() == seasonNumber).findFirst().orElse(null);
  }
}

package hs.mediasystem.ext.basicmediatypes.domain;

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

  private final List<Keyword> keywords;
  private final State state;
  private final List<Season> seasons;
  private final LocalDate lastAirDate;

  /**
   * Constructs a new instance.
   *
   * @param identifier a {@link ProductionIdentifier}, cannot be null
   * @param details a {@link Details}, cannot be null
   * @param reception a {@link Reception}, can be null
   * @param languages a list of language codes, cannot be null but can be empty
   * @param genres a list of genres, cannot be null but can be empty
   * @param keywords a list of keywords, cannot be null but can be empty
   * @param state a {@link State}, can be null if unknown
   * @param lastAirDate a last air date, can be null if unknown
   * @param popularity a popularity value
   * @param seasons the seasons this serie consists of, can be null if unknown (due to partial information) and can be empty (if known there are no seasons)
   */
  public Serie(ProductionIdentifier identifier, Details details, Reception reception, List<String> languages, List<String> genres, List<Keyword> keywords, State state, LocalDate lastAirDate, double popularity, List<Season> seasons) {
    super(identifier, details, reception, languages, genres, popularity);

    this.keywords = keywords;
    this.state = state;
    this.lastAirDate = lastAirDate;
    this.seasons = seasons == null ? null : new ArrayList<>(Collections.unmodifiableList(seasons));
  }

  public List<Keyword> getKeywords() {
    return keywords;
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

  public Season findSeason(int seasonNumber) {
    return seasons == null ? null : seasons.stream().filter(s -> s.getNumber() == seasonNumber).findFirst().orElse(null);
  }
}

package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
  public Serie(ProductionIdentifier identifier, Details details, Reception reception, List<String> languages, List<String> genres, List<Keyword> keywords, State state, LocalDate lastAirDate, double popularity, List<Season> seasons, Set<Identifier> relatedIdentifiers) {
    super(identifier, details, reception, languages, genres, popularity, relatedIdentifiers);

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

  public Optional<Episode> findEpisode(int seasonNumber, int episodeNumber) {
    return seasons == null ? null : seasons.stream().filter(s -> s.getNumber() == seasonNumber).map(Season::getEpisodes).flatMap(Collection::stream).filter(e -> e.getNumber() == episodeNumber).findFirst();
  }

  public Optional<Episode> findNextEpisode(Episode episode) {
    return findEpisode(episode.getSeasonNumber(), episode.getNumber() + 1)
      .or(() -> findEpisode(episode.getSeasonNumber() + 1, 1));
  }
}

package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;

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

  private final State state;
  private final List<Season> seasons;
  private final LocalDate lastAirDate;

  /**
   * Constructs a new instance.
   *
   * @param id a {@link WorkId}, cannot be null
   * @param details a {@link Details}, cannot be null
   * @param tagLine a tag line, can be null
   * @param reception a {@link Reception}, can be null
   * @param classification a {@link Classification}, cannot be null
   * @param state a {@link State}, can be null if unknown
   * @param lastAirDate a last air date, can be null if unknown
   * @param popularity a popularity value
   * @param seasons the seasons this serie consists of, can be null if unknown (due to partial information) and can be empty (if known there are no seasons)
   * @param relatedWorks a set of related {@link WorkId}s, cannot be {@code null}
   */
  public Serie(WorkId id, Details details, String tagLine, Reception reception, Classification classification, State state, LocalDate lastAirDate, double popularity, List<Season> seasons, Set<WorkId> relatedWorks) {
    super(id, details, tagLine, reception, classification, popularity, relatedWorks);

    this.state = state;
    this.lastAirDate = lastAirDate;
    this.seasons = seasons == null ? null : new ArrayList<>(Collections.unmodifiableList(seasons));
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

  public Optional<Season> findSeason(int seasonNumber) {
    return seasons == null ? Optional.empty() : seasons.stream().filter(s -> s.getNumber() == seasonNumber).findFirst();
  }

  public Optional<Episode> findEpisode(int seasonNumber, int episodeNumber) {
    return seasons == null ? null : seasons.stream().filter(s -> s.getNumber() == seasonNumber).map(Season::getEpisodes).flatMap(Collection::stream).filter(e -> e.getNumber() == episodeNumber).findFirst();
  }

  public Optional<Episode> findNextEpisode(Episode episode) {
    return findEpisode(episode.getSeasonNumber(), episode.getNumber() + 1)
      .or(() -> findEpisode(episode.getSeasonNumber() + 1, 1));
  }
}

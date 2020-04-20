package hs.mediasystem.ui.api.domain;

import java.time.LocalDate;
import java.util.Optional;

public class Serie {
  private Optional<LocalDate> lastAirDate;
  private Optional<Integer> totalSeasons;
  private Optional<Integer> totalEpisodes;

  public Serie(LocalDate lastAirDate, Integer totalSeasons, Integer totalEpisodes) {
    this.lastAirDate = Optional.ofNullable(lastAirDate);
    this.totalSeasons = Optional.ofNullable(totalSeasons);
    this.totalEpisodes = Optional.ofNullable(totalEpisodes);
  }

  public Optional<LocalDate> getLastAirDate() {
    return lastAirDate;
  }

  public Optional<Integer> getTotalSeasons() {
    return totalSeasons;
  }

  public Optional<Integer> getTotalEpisodes() {
    return totalEpisodes;
  }
}

package hs.mediasystem.ui.api.domain;

import java.time.LocalDate;
import java.util.Optional;

public class Serie {
  private Optional<LocalDate> lastAirDate;
  private Optional<Integer> totalSeasons;
  private int totalEpisodes;

  public Serie(LocalDate lastAirDate, Integer totalSeasons, int totalEpisodes) {
    this.lastAirDate = Optional.ofNullable(lastAirDate);
    this.totalSeasons = Optional.ofNullable(totalSeasons);
    this.totalEpisodes = totalEpisodes;
  }

  public Optional<LocalDate> getLastAirDate() {
    return lastAirDate;
  }

  public Optional<Integer> getTotalSeasons() {
    return totalSeasons;
  }

  public int getTotalEpisodes() {
    return totalEpisodes;
  }
}

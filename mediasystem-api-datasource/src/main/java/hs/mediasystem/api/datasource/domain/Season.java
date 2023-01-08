package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.WorkId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Season extends Release {
  private final int number;
  private final List<Episode> episodes;

  public Season(WorkId id, Details details, int number, List<Episode> episodes) {
    super(id, details, null);

    this.number = number;
    this.episodes = new ArrayList<>(Collections.unmodifiableList(episodes));
  }

  public int getNumber() {
    return number;
  }

  public List<Episode> getEpisodes() {
    return episodes;
  }

  public Episode findEpisode(int number) {
    return episodes.stream().filter(s -> s.getNumber() == number).findFirst().orElse(null);
  }
}

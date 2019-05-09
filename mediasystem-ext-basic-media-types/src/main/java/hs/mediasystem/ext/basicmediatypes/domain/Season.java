package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Season extends Release {
  private final int number;
  private final List<Episode> episodes;

  public Season(ProductionIdentifier identifier, Details details, int number, List<Episode> episodes) {
    super(identifier, details, null);

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

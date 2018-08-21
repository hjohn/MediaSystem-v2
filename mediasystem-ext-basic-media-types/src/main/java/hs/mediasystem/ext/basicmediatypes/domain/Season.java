package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Season extends Production {
  private final int number;
  private final List<Episode> episodes;

  public Season(ProductionIdentifier identifier, Details details, int number, List<Episode> episodes) {
    super(identifier, details, null, null, Collections.emptyList(), Collections.emptyList());

    this.number = number;
    this.episodes = new ArrayList<>(Collections.unmodifiableList(episodes));
  }

  public int getNumber() {
    return number;
  }

  public List<Episode> getEpisodes() {
    return episodes;
  }
}

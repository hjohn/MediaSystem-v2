package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Season {
  private final int number;
  private final Production production;
  private final List<Episode> episodes;

  public Season(int number, Production production, List<Episode> episodes) {
    this.number = number;
    this.production = production;
    this.episodes = new ArrayList<>(Collections.unmodifiableList(episodes));
  }

  public int getNumber() {
    return number;
  }

  public Production getProduction() {
    return production;
  }

  public List<Episode> getEpisodes() {
    return episodes;
  }
}

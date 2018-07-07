package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Season;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Serie extends AbstractProductionDescriptor {
  private final List<Season> seasons;

  public Serie(Production production,List<Season> seasons) {
    super(production);

    this.seasons = new ArrayList<>(Collections.unmodifiableList(seasons));
  }

  public List<Season> getSeasons() {
    return seasons;
  }

  public Season findSeason(int seasonNumber) {
    return seasons.stream().filter(s -> s.getNumber() == seasonNumber).findFirst().orElse(null);
  }
}

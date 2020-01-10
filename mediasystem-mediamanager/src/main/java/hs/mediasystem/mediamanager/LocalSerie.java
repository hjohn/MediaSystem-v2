package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class LocalSerie extends Serie {
  private final List<Episode> extras;

  public LocalSerie(ProductionIdentifier identifier, Details details, Reception reception, List<String> languages, List<String> genres, List<Keyword> keywords, State state, LocalDate lastAirDate, double popularity, List<Season> seasons, Set<Identifier> relatedIdentifiers, List<Episode> extras) {
    super(identifier, details, reception, languages, genres, keywords, state, lastAirDate, popularity, seasons, relatedIdentifiers);

    this.extras = extras;
  }

  public List<Episode> getExtras() {
    return extras;
  }
}

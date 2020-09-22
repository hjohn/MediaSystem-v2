package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Serie.State;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Series {

  public static Serie create() {
    return create(new ArrayList<>());
  }

  public static Serie create(List<Episode> episodes) {
    return create(new ProductionIdentifier(DataSource.instance(MediaType.SERIE, "TMDB"), "12345"), "Charmed", episodes);
  }

  public static Serie create(ProductionIdentifier identifier, String title, List<Episode> episodes) {
    return new Serie(
      identifier,
      new Details(title, "subtitle", "Power of 3", LocalDate.of(2003, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
      new Reception(8, 12345),
      new Classification(
        Arrays.asList("Action", "Science-Fiction"),
        Arrays.asList("en"),
        Arrays.asList(new Keyword(new Identifier(DataSource.instance(MediaType.KEYWORD, "TMDB"), "12345"), "magic")),
        Map.of(),
        false
      ),
      State.ENDED,
      LocalDate.of(2009, 6, 6),
      99.0,
      Arrays.asList(
        new Season(
          new ProductionIdentifier(DataSource.instance(MediaType.SEASON, "TMDB"), "1"),
          new Details("Season 1", "subtitle", "", LocalDate.of(2003, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
          1,
          episodes
        )
      ),
      Set.of()
    );
  }
}

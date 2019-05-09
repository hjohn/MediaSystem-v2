package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.Serie.State;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Series {

  public static Serie create() {
    return create(new ArrayList<>());
  }

  public static Serie create(List<Episode> episodes) {
    return new Serie(
      new ProductionIdentifier(DataSource.instance(MediaType.of("SERIE"), "TMDB"), "12345"),
      new Details("Charmed", "Power of 3", LocalDate.of(2003, 6, 6), new ImageURI("http://localhost"), new ImageURI("http://localhost")),
      new Reception(8, 12345),
      Arrays.asList("en"),
      Arrays.asList("Action", "Science-Fiction"),
      Arrays.asList(new Keyword(new Identifier(DataSource.instance(MediaType.of("KEYWORD"), "TMDB"), "12345"), "magic")),
      State.ENDED,
      LocalDate.of(2009, 6, 6),
      99.0,
      Arrays.asList(
        new Season(
          new ProductionIdentifier(DataSource.instance(MediaType.of("SEASON"), "TMDB"), "1"),
          new Details("Season 1", "", LocalDate.of(2003, 6, 6), new ImageURI("http://localhost"), new ImageURI("http://localhost")),
          1,
          episodes
        )
      )
    );
  }
}

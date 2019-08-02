package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Movie.State;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;

public class Movies {
  public static Movie create() {
    return create(new ProductionIdentifier(DataSource.instance(MediaType.of("MOVIE"), "TMDB"), "12345"));
  }

  public static Movie create(ProductionIdentifier identifier) {
    return new Movie(
      identifier,
      new Details("The Terminator", "Robot kills humans", LocalDate.of(1984, 6, 6), new ImageURI("http://localhost"), new ImageURI("http://localhost")),
      new Reception(8, 12345),
      Duration.ofHours(2),
      Arrays.asList("en"),
      Arrays.asList("Action", "Science-Fiction"),
      Arrays.asList(new Keyword(new Identifier(DataSource.instance(MediaType.of("KEYWORD"), "TMDB"), "12345"), "timetravel")),
      99.0,
      "Skynet comes",
      State.RELEASED,
      null,
      Set.of()
    );
  }
}

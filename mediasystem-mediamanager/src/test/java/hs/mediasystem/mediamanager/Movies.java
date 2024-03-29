package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Keyword;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Movie.State;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class Movies {
  public static Movie create() {
    return create(new ProductionIdentifier(DataSource.instance(MediaType.MOVIE, "TMDB"), "12345"));
  }

  public static Movie create(ProductionIdentifier identifier) {
    return create(identifier, "The Terminator");
  }

  public static Movie create(ProductionIdentifier identifier, String title) {
    return new Movie(
      identifier,
      new Details(title, "subtitle", "Robot kills humans", LocalDate.of(1984, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
      new Reception(8, 12345),
      Duration.ofHours(2),
      new Classification(
        Arrays.asList("Action", "Science-Fiction"),
        Arrays.asList("en"),
        Arrays.asList(new Keyword(new Identifier(DataSource.instance(MediaType.KEYWORD, "TMDB"), "12345"), "timetravel")),
        Map.of(),
        false
      ),
      99.0,
      "Skynet comes",
      State.RELEASED,
      Set.of()
    );
  }
}

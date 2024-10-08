package hs.mediasystem.db.util;

import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Keyword;
import hs.mediasystem.api.datasource.domain.Movie;
import hs.mediasystem.api.datasource.domain.Movie.State;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.KeywordId;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.image.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

public class Movies {
  public static Movie create() {
    return create(new WorkId(DataSource.instance("TMDB"), MediaType.MOVIE, "12345"));
  }

  public static Movie create(WorkId id) {
    return create(id, "The Terminator");
  }

  public static Movie create(WorkId id, String title) {
    return new Movie(
      id,
      new Details(title, "subtitle", "Robot kills humans", LocalDate.of(1984, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
      new Reception(8, 12345),
      null,
      "Skynet comes",
      Duration.ofHours(2),
      new Classification(
        Arrays.asList("Action", "Science-Fiction"),
        Arrays.asList("en"),
        Arrays.asList(new Keyword(new KeywordId(DataSource.instance("TMDB"), "12345"), "timetravel")),
        Map.of(),
        false
      ),
      99.0,
      State.RELEASED
    );
  }
}

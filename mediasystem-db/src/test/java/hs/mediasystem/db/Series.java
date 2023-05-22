package hs.mediasystem.db;

import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Episode;
import hs.mediasystem.api.datasource.domain.Keyword;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.domain.Serie.State;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.KeywordId;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.image.ImageURI;

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
    return create(new WorkId(DataSource.instance("TMDB"), MediaType.SERIE, "12345"), "Charmed", episodes);
  }

  public static Serie create(WorkId id, String title, List<Episode> episodes) {
    return new Serie(
      id,
      new Details(title, "subtitle", "Power of 3", LocalDate.of(2003, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
      "Could they be witches?",
      new Reception(8, 12345),
      new Classification(
        Arrays.asList("Action", "Science-Fiction"),
        Arrays.asList("en"),
        Arrays.asList(new Keyword(new KeywordId(DataSource.instance("TMDB"), "12345"), "magic")),
        Map.of(),
        false
      ),
      State.ENDED,
      LocalDate.of(2009, 6, 6),
      99.0,
      Arrays.asList(
        new Serie.Season(
          new WorkId(DataSource.instance("TMDB"), MediaType.SEASON, "1"),
          new Details("Season 1", "subtitle", "", LocalDate.of(2003, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
          1,
          episodes.stream().map(Series::toSerieEpisode).toList()
        )
      ),
      Set.of()
    );
  }

  private static Serie.Episode toSerieEpisode(Episode episode) {
    return new Serie.Episode(
      episode.getId(),
      episode.getDetails(),
      episode.getReception(),
      episode.getDuration(),
      episode.getSeasonNumber(),
      episode.getNumber(),
      episode.getPersonRoles()
    );
  }
}

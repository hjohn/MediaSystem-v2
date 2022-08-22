package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

public class Episodes {
  public static Episode create() {
    return create("12/2/12345");
  }

  public static Episode create(WorkId id) {
    return create(id, 1, 5);
  }

  public static Episode create(String id) {
    return create(id, 1, 5);
  }

  public static Episode create(String id, int season, int episodeNumber) {
    return create(
      new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, id),
      season,
      episodeNumber
    );
  }

  public static Episode create(WorkId id, int season, int episodeNumber) {
    return create(id, "The Nightwatch", season, episodeNumber);
  }

  public static Episode create(WorkId id, String title, int season, int episodeNumber) {
    return new Episode(
      id,
      new Details(title, "Subtitle", "Stuff", LocalDate.of(2012, 6, 6), null, new ImageURI("http://localhost", "key"), new ImageURI("http://localhost", "key")),
      new Reception(8, 12345),
      Duration.ofMinutes(40),
      season,
      episodeNumber,
      Collections.emptyList()
    );
  }
}

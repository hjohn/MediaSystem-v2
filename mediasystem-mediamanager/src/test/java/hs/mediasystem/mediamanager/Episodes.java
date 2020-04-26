package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

public class Episodes {
  public static Episode create() {
    return create("12/2/12345");
  }

  public static Episode create(EpisodeIdentifier identifier) {
    return create(identifier, 1, 5);
  }

  public static Episode create(String id) {
    return create(id, 1, 5);
  }

  public static Episode create(String id, int season, int episodeNumber) {
    return create(
      new EpisodeIdentifier(DataSource.instance(MediaType.EPISODE, "TMDB"), id),
      season,
      episodeNumber
    );
  }

  public static Episode create(EpisodeIdentifier identifier, int season, int episodeNumber) {
    return create(identifier, "The Nightwatch", season, episodeNumber);
  }

  public static Episode create(EpisodeIdentifier identifier, String title, int season, int episodeNumber) {
    return new Episode(
      identifier,
      new Details(title, "Subtitle", "Stuff", LocalDate.of(2012, 6, 6), new ImageURI("http://localhost", "key"), new ImageURI("http://localhost", "key")),
      new Reception(8, 12345),
      Duration.ofMinutes(40),
      season,
      episodeNumber,
      Collections.emptyList()
    );
  }
}

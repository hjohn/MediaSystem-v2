package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Reception;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

public class Episodes {
  public static Episode create() {
    return create("12:2:12345");
  }

  public static Episode create(ProductionIdentifier identifier) {
    return create(identifier, 1, 5);
  }

  public static Episode create(String id) {
    return create(id, 1, 5);
  }

  public static Episode create(String id, int season, int episodeNumber) {
    return create(
      new ProductionIdentifier(DataSource.instance(MediaType.of("EPISODE"), "TMDB"), id),
      season,
      episodeNumber
    );
  }

  public static Episode create(ProductionIdentifier identifier, int season, int episodeNumber) {
    return new Episode(
      identifier,
      new Details("The Nightwatch", "Stuff", LocalDate.of(2012, 6, 6), new ImageURI("http://localhost"), new ImageURI("http://localhost")),
      new Reception(8, 12345),
      Duration.ofMinutes(40),
      season,
      episodeNumber,
      Collections.emptyList()
    );
  }
}

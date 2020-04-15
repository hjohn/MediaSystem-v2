package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;

public class Productions {

  public static Production create() {
    return new Production(
      new ProductionIdentifier(DataSource.instance(MediaType.of("MOVIE"), "TMDB"), "12345"),
      new Details("The Terminator", "Subtitle", "Robot kills humans", LocalDate.of(1984, 6, 6), new ImageURI("http://localhost"), new ImageURI("http://localhost")),
      new Reception(8, 12345),
      Arrays.asList("en"),
      Arrays.asList("Action", "Science-Fiction"),
      20.0,
      Set.of()
    );
  }
}

package hs.mediasystem.db.util;

import hs.mediasystem.api.datasource.domain.Classification;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.image.ImageURI;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Productions {

  public static Production create() {
    return new Production(
      new WorkId(DataSource.instance("TMDB"), MediaType.MOVIE, "12345"),
      new Details("The Terminator", "Subtitle", "Robot kills humans", LocalDate.of(1984, 6, 6), new ImageURI("http://localhost", "key"), null, new ImageURI("http://localhost", "key")),
      new Reception(8, 12345),
      null,
      "Skynet Comes",
      new Classification(
        Arrays.asList("Action", "Science-Fiction"),
        Arrays.asList("en"),
        List.of(),
        Map.of(),
        false
      ),
      20.0
    );
  }
}

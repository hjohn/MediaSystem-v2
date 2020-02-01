package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple.Tuple2;

public interface IdentificationService {
  DataSource getDataSource();

  /**
   * Attempt an identification of the given {@link Attributes} with this service.
   *
   * @param stream the {@link BasicStream}
   * @return an {@link Identifier}, {@link Match} tuple, or <code>null</code> if unable to identify
   */
  Tuple2<Identifier, Match> identify(BasicStream stream);
}

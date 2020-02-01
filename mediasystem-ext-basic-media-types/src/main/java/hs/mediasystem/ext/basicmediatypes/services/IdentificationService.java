package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.BasicStream;

import java.util.Map;

public interface IdentificationService {
  DataSource getDataSource();

  /**
   * Attempt an identification of the given {@link BasicStream} with this service.
   *
   * @param stream the {@link BasicStream}
   * @return a {@link Map} linking {@link StreamID} with a tuple of {@link Identifier}, {@link Match} tuple, or an empty map if unable to identify
   */
  Map<StreamID, Identification> identify(BasicStream stream);
}

package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple.Tuple2;

public interface IdentificationService {
  DataSource getDataSource();

  /**
   * Attempt an identification of the given {@link Attributes} with this service.
   *
   * @param stream the {@link BasicStream}
   * @return an {@link Identifier}, {@link Identification} tuple, or <code>null</code> if unable to identify
   */
  Tuple2<Identifier, Identification> identify(BasicStream stream);
}

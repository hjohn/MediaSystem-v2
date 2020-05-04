package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.io.IOException;

public interface QueryService {
  DataSource getDataSource();

  /**
   * Queries a descriptor service and returns a {@link MediaDescriptor} if
   * found or throws an exception otherwise.
   *
   * @param identifier an {@link Identifier}, cannot be null
   * @return a {@link MediaDescriptor}, never null
   */
  MediaDescriptor query(Identifier identifier) throws IOException;
}

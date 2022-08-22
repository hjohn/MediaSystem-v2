package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;

import java.io.IOException;

public interface QueryService {
  DataSource getDataSource();
  MediaType getMediaType();

  /**
   * Queries a descriptor service and returns a {@link WorkDescriptor} if
   * found or throws an exception otherwise.
   *
   * @param id an {@link WorkId}, cannot be null
   * @return a {@link WorkDescriptor}, never null
   * @throws IOException when an I/O problem occurred
   */
  WorkDescriptor query(WorkId id) throws IOException;
}

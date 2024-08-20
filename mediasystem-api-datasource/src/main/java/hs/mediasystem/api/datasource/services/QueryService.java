package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;

import java.io.IOException;
import java.util.Optional;

public interface QueryService {
  DataSource getDataSource();

  /**
   * Queries a descriptor service and returns an optional {@link WorkDescriptor} or
   * empty if not found. An exception is thrown if the service could not be contacted
   * at all.
   *
   * @param id an {@link WorkId}, cannot be {@code null}
   * @return an optional {@link WorkDescriptor}, never {@code null}
   * @throws IOException when an I/O problem occurred
   */
  Optional<? extends WorkDescriptor> query(WorkId id) throws IOException;
}

package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.io.IOException;
import java.util.Optional;

public interface IdentificationService {
  DataSource getDataSource();
  MediaType getMediaType();

  /**
   * Attempt an identification of the given {@link Streamable} and optional parent descriptor with this service.
   *
   * @param streamable a {@link Streamable}, never null
   * @param parent a parent descriptor, can be null
   * @return an {@link Identification}, never null
   * @throws IOException when an I/O problem occurred
   */
  Optional<Identification> identify(Streamable streamable, WorkDescriptor parent) throws IOException;
}

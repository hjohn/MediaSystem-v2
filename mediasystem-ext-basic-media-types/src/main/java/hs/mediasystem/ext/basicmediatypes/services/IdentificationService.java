package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.util.Optional;

public interface IdentificationService {
  DataSource getDataSource();

  /**
   * Attempt an identification of the given {@link Streamable} and optional parent descriptor with this service.
   *
   * @param streamable a {@link Streamable}, never null
   * @param parent a parent descriptor, can be null
   * @return an {@link Identification}, never null
   */
  Optional<Identification> identify(Streamable streamable, MediaDescriptor parent);
}

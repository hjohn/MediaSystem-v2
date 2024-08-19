package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A service for identifying discoveries.
 */
public interface IdentificationProvider {
  static final MinimalIdentificationProvider MINIMAL_PROVIDER = new MinimalIdentificationProvider();

  class MinimalIdentificationProvider implements IdentificationProvider {

    @Override
    public String getName() {
      return "dummy";
    }

    @Override
    public Optional<Identification> identify(Discovery discovery) {
      return Optional.of(identifyChild(discovery, null));
    }

    @Override
    public Identification identifyChild(Discovery discovery, WorkDescriptor parent) {
      Release descriptor = MinimalWorkSupport.createMinimalDescriptor(discovery);
      Match match = new Match(Type.NONE, 1.0f, discovery.lastModificationTime());

      return new Identification(List.of(descriptor), match);
    }
  }

  /**
   * The name of the data source this service represents.
   *
   * @return a name, never {@code null}
   */
  String getName();

  /**
   * Identifies the given discovery.<p>
   *
   * If the identification cannot be completed, an exception is thrown. If the
   * identification completed, but there was no match, an empty optional is
   * returned. Otherwise an {@link Identification} is returned.
   *
   * @param discovery a {@link Discovery} to identify, cannot be {@code null}
   * @return an optional {@link Identification}, never {@code null}
   * @throws IOException when an IO exception occurred
   */
  Optional<Identification> identify(Discovery discovery) throws IOException;

  /**
   * Optional operation that can identify a child of an earlier identified parent.
   * Only providers which need more context for identifying a child support this
   * operation (for example, to identify an episode, one must know which serie
   * it belongs to).<p>
   *
   * A result must always be provided even if it is just a minimal identification.
   *
   * @param discovery a {@link Discovery} to identify, cannot be {@code null}
   * @param parent a parent descriptor, cannot be {@code null}
   * @return an {@link Identification}, never {@code null}
   */
  default Identification identifyChild(Discovery discovery, WorkDescriptor parent) {
    throw new UnsupportedOperationException();
  }

}

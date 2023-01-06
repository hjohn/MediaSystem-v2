package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.api.Discovery;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * A service for identifying discoveries.
 */
public interface IdentificationService {

  /**
   * Holds the result of an identification.
   *
   * @param descriptors one or more descriptors that matched the discovery, cannot be {@code null} or empty
   * @param match a {@link Match}, cannot be {@code null}
   */
  record Identification(List<? extends WorkDescriptor> descriptors, Match match) {
    public Identification {
      if(descriptors == null) {
        throw new IllegalArgumentException("descriptors cannot be null");
      }
      if(match == null) {
        throw new IllegalArgumentException("match cannot be null");
      }
      if(descriptors.isEmpty()) {
        throw new IllegalArgumentException("descriptors cannot be empty");
      }
    }
  }

  /**
   * The name of the data source this service represents.
   *
   * @return a name, never {@code null}
   */
  String getName();

  /**
   * Identifies the given discovery. If the discovery is a child of another
   * discovery, the descriptor resulting from it is always provided, otherwise
   * it is {@code null}.<p>
   *
   * If the identification cannot be completed, an exception is thrown. If the
   * identification completed, but there was no match, an empty optional is
   * returned. Otherwise an {@link Identification} is returned.
   *
   * @param discovery a {@link Discovery} to identify, cannot be {@code null}
   * @param parent a parent descriptor, can be {@code null}
   * @return an optional {@link Identification}, never {@code null}
   * @throws IOException when an IO exception occurred
   */
  Optional<Identification> identify(Discovery discovery, WorkDescriptor parent) throws IOException;
}

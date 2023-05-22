package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.work.Match;

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
   * @param releases one or more releases that matched the discovery, cannot be {@code null} or empty
   * @param match a {@link Match}, cannot be {@code null}
   */
  record Identification(List<? extends Release> releases, Match match) {
    public Identification {
      if(releases == null) {
        throw new IllegalArgumentException("releases cannot be null");
      }
      if(match == null) {
        throw new IllegalArgumentException("match cannot be null");
      }
      if(releases.isEmpty()) {
        throw new IllegalArgumentException("releases cannot be empty");
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

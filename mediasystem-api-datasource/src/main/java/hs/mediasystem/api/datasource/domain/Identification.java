package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.Match;

import java.util.List;

/**
 * Holds the result of an identification.
 *
 * @param releases one or more releases that matched the discovery, cannot be {@code null} or empty
 * @param match a {@link Match}, cannot be {@code null}
 */
public record Identification(List<? extends Release> releases, Match match) {
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
package hs.mediasystem.domain.work;

import java.time.Instant;

public record Match(Type type, float accuracy, Instant creationTime) {

  public enum Type {

    /**
     * Matched manually by user.
     */
    MANUAL,

    /**
     * Matched directly by a provider specific id (like an IMDB id parsed from file name).
     */
    ID,

    /**
     * Matched derived from another match, for example when an identification (for another source)
     * is returned by a match from the current source.  These kinds of matches are only as
     * trustworthy as the original match.
     */
    DERIVED,

    /**
     * Matched by a hash calculated over the content of a file.
     */
    HASH,

    /**
     * Matched on name and release date.
     */
    NAME_AND_RELEASE_DATE,

    /**
     * Matched on name only.
     */
    NAME,

    /**
     * There was no match.
     */
    NONE
  }

  public Match {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(accuracy < 0 || accuracy > 1) {
      throw new IllegalArgumentException("accuracy must be between 0 and 1.0: " + accuracy);
    }
    if(creationTime == null) {
      throw new IllegalArgumentException("creationTime cannot be null");
    }
  }

  @Override
  public String toString() {
    return "Match[" + type + " @ " + accuracy * 100 + "%]";
  }
}

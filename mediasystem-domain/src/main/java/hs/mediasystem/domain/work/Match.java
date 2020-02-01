package hs.mediasystem.domain.work;

import java.time.Instant;
import java.util.Objects;

public class Match {

  public enum MatchType {  // FIXME give simpler names

    /**
     * Matched manually by user.
     */
    MANUAL,

    /**
     * Matched directly by a provider specific id (like an imdbid parsed from file name).
     */
    ID,

    /**
     * Matched derived from another match, for example when an identifier (for another source)
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
    NAME
  }

  private final MatchType matchType;
  private final double matchAccuracy;
  private final Instant creationTime;

  public Match(MatchType matchType, double matchAccuracy, Instant creationTime) {
    if(matchType == null) {
      throw new IllegalArgumentException("matchType cannot be null");
    }
    if(matchAccuracy < 0 || matchAccuracy > 1) {
      throw new IllegalArgumentException("matchAccuracy must be between 0 and 1.0: " + matchAccuracy);
    }
    if(creationTime == null) {
      throw new IllegalArgumentException("creationTime cannot be null");
    }

    this.matchType = matchType;
    this.matchAccuracy = matchAccuracy;
    this.creationTime = creationTime;
  }

  public MatchType getMatchType() {
    return matchType;
  }

  public double getMatchAccuracy() {
    return matchAccuracy;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(matchType, matchAccuracy, creationTime);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Match other = (Match)obj;

    if(Double.doubleToLongBits(matchAccuracy) != Double.doubleToLongBits(other.matchAccuracy)) {
      return false;
    }
    if(matchType != other.matchType) {
      return false;
    }
    if(creationTime != other.creationTime) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "Match[" + matchType + " @ " + matchAccuracy * 100 + "%]";
  }
}

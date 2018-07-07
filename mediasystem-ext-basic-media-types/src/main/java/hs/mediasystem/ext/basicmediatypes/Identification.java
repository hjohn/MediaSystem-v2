package hs.mediasystem.ext.basicmediatypes;

import java.util.Objects;

public class Identification {

  public enum MatchType {

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

  private final Identifier identifier;
  private final MatchType matchType;
  private final double matchAccuracy;

  public Identification(Identifier identifier, MatchType matchType, double matchAccuracy) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(matchType == null) {
      throw new IllegalArgumentException("matchType cannot be null");
    }
    if(matchAccuracy < 0 || matchAccuracy > 1) {
      throw new IllegalArgumentException("matchAccuracy must be between 0 and 1.0: " + matchAccuracy);
    }

    this.identifier = identifier;
    this.matchType = matchType;
    this.matchAccuracy = matchAccuracy;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public MatchType getMatchType() {
    return matchType;
  }

  public double getMatchAccuracy() {
    return matchAccuracy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, matchType, matchAccuracy);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Identification other = (Identification)obj;

    if(!identifier.equals(other.identifier)) {
      return false;
    }
    if(Double.doubleToLongBits(matchAccuracy) != Double.doubleToLongBits(other.matchAccuracy)) {
      return false;
    }
    if(matchType != other.matchType) {
      return false;
    }

    return true;
  }


}

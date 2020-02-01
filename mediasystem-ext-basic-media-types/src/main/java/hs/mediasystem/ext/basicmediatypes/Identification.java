package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.util.Objects;

public class Identification {
  private final Identifier identifier;
  private final Match match;

  public Identification(Identifier identifier, Match match) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }

    this.identifier = identifier;
    this.match = match;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public Match getMatch() {
    return match;
  }

  @Override
  public String toString() {
    return "[" + identifier + ", " + match + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, match);
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
    if(!match.equals(other.match)) {
      return false;
    }

    return true;
  }


}

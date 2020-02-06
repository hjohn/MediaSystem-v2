package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Identification {
  private final List<Identifier> identifiers;
  private final Match match;

  public Identification(List<Identifier> identifiers, Match match) {
    if(identifiers == null || identifiers.isEmpty()) {
      throw new IllegalArgumentException("identifiers cannot be null or empty: " + identifiers);
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }

    this.identifiers = Collections.unmodifiableList(identifiers);
    this.match = match;
  }

  public Identifier getPrimaryIdentifier() {
    return identifiers.get(0);
  }

  public List<Identifier> getIdentifiers() {
    return identifiers;
  }

  public Match getMatch() {
    return match;
  }

  @Override
  public String toString() {
    return "[" + identifiers + ", " + match + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifiers, match);
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

    if(!identifiers.equals(other.identifiers)) {
      return false;
    }
    if(!match.equals(other.match)) {
      return false;
    }

    return true;
  }
}

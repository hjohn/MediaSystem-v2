package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.WorkId;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Identification {
  private final List<WorkId> ids;
  private final Match match;

  public Identification(List<WorkId> ids, Match match) {
    if(ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException("ids cannot be null or empty: " + ids);
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }

    this.ids = Collections.unmodifiableList(ids);
    this.match = match;
  }

  public WorkId getPrimaryWorkId() {
    return ids.get(0);
  }

  public List<WorkId> getWorkIds() {
    return ids;
  }

  public Match getMatch() {
    return match;
  }

  @Override
  public String toString() {
    return "[" + ids + ", " + match + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(ids, match);
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

    if(!ids.equals(other.ids)) {
      return false;
    }
    if(!match.equals(other.match)) {
      return false;
    }

    return true;
  }
}

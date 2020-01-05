package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.scanner.api.MediaType;

public class WorkId {
  private final Identifier identifier;

  public WorkId(Identifier identifier) {
    this.identifier = identifier;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public MediaType getType() {
    return identifier.getDataSource().getType();
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    WorkId other = (WorkId)obj;

    if(!identifier.equals(other.identifier)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return identifier.toString();
  }
}

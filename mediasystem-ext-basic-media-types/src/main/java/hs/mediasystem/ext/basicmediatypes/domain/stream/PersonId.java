package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.ext.basicmediatypes.Identifier;

public class PersonId {
  private final Identifier identifier;

  public PersonId(Identifier identifier) {
    this.identifier = identifier;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  @Override
  public String toString() {
    return identifier.toString();
  }
}

package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;

public class CollectionDetails {
  private final Identifier identifier;
  private final Details details;

  public CollectionDetails(Identifier identifier, Details details) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }

    this.identifier = identifier;
    this.details = details;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public Details getDetails() {
    return details;
  }
}

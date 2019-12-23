package hs.mediasystem.runner.db;

import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.runner.collection.CollectionDefinition;

public class Collection {
  private final Details details;
  private final CollectionDefinition definition;

  public Collection(Details details, CollectionDefinition definition) {
    this.details = details;
    this.definition = definition;
  }

  public Details getDetails() {
    return details;
  }

  public CollectionDefinition getDefinition() {
    return definition;
  }
}

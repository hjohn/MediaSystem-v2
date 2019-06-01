package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;

import java.util.List;

public class IdentifierCollection extends AbstractCollection<Identifier> {

  public IdentifierCollection(CollectionDetails collectionDetails, List<Identifier> items) {
    super(collectionDetails, items);
  }

}

package hs.mediasystem.ext.basicmediatypes.domain;

import java.util.List;

public class ProductionCollection extends AbstractCollection<Production> {

  public ProductionCollection(CollectionDetails collectionDetails, List<Production> items) {
    super(collectionDetails, items);
  }
}

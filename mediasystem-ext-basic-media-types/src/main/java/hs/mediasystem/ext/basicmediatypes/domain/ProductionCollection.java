package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.util.ImageURI;

import java.util.Collections;
import java.util.List;

public class ProductionCollection extends AbstractCollection<Production> implements Partial {
  private final boolean complete;

  protected ProductionCollection(boolean complete, CollectionDetails collectionDetails, List<Production> items) {
    super(collectionDetails, items);

    this.complete = complete;
  }

  public ProductionCollection(CollectionDetails collectionDetails, List<Production> productions) {
    this(true, collectionDetails, productions);
  }

  public ProductionCollection(Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Production> productions) {
    this(true, new CollectionDetails(identifier, new Details(name, overview, null, image, backdrop)), productions);
  }

  public ProductionCollection(Identifier identifier, String name, ImageURI image, ImageURI backdrop) {
    this(false, new CollectionDetails(identifier, new Details(name, null, null, image, backdrop)), Collections.emptyList());
  }

  @Override
  public boolean isComplete() {
    return complete;
  }
}

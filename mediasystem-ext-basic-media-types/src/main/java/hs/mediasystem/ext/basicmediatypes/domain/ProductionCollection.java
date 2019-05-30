package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.util.ImageURI;

import java.util.Collections;
import java.util.List;

public class ProductionCollection extends AbstractCollection<Production> implements Partial {
  private final boolean complete;

  protected ProductionCollection(boolean complete, Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Production> productions) {
    super(identifier, name, overview, image, backdrop, productions);

    this.complete = complete;
  }

  public ProductionCollection(Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Production> productions) {
    this(true, identifier, name, overview, image, backdrop, productions);
  }

  public ProductionCollection(Identifier identifier, String name, ImageURI image, ImageURI backdrop) {
    this(false, identifier, name, null, image, backdrop, Collections.emptyList());
  }

  @Override
  public boolean isComplete() {
    return complete;
  }
}

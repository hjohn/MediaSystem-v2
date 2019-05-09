package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductionCollection implements Partial {
  private final Identifier identifier;
  private final String name;
  private final String overview;
  private final ImageURI image;
  private final ImageURI backdrop;
  private final boolean complete;
  private final List<Production> productions;

  protected ProductionCollection(boolean complete, Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Production> productions) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    if(complete && productions.isEmpty()) {
      throw new IllegalArgumentException("productions cannot be empty");
    }
    if(productions.contains(null)) {
      throw new IllegalArgumentException("productions cannot contain null");
    }

    this.complete = complete;
    this.identifier = identifier;
    this.name = name;
    this.overview = overview;
    this.image = image;
    this.backdrop = backdrop;
    this.productions = Collections.unmodifiableList(new ArrayList<>(productions));
  }

  public ProductionCollection(Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<Production> productions) {
    this(true, identifier, name, overview, image, backdrop, productions);
  }

  public ProductionCollection(Identifier identifier, String name, ImageURI image, ImageURI backdrop) {
    this(false, identifier, name, null, image, backdrop, Collections.emptyList());
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public String getName() {
    return name;
  }

  public String getOverview() {
    return overview;
  }

  public ImageURI getImage() {
    return image;
  }

  public ImageURI getBackdrop() {
    return backdrop;
  }

  public List<Production> getProductions() {
    return productions;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }
}

package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.util.ImageURI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCollection<T> implements MediaDescriptor {
  private final Identifier identifier;
  private final String name;
  private final String overview;
  private final ImageURI image;
  private final ImageURI backdrop;
  private final List<T> items;

  public AbstractCollection(Identifier identifier, String name, String overview, ImageURI image, ImageURI backdrop, List<T> items) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name cannot be null or empty");
    }
    if(items.isEmpty()) {
      throw new IllegalArgumentException("items cannot be empty");
    }
    if(items.contains(null)) {
      throw new IllegalArgumentException("items cannot contain null");
    }

    this.identifier = identifier;
    this.name = name;
    this.overview = overview;
    this.image = image;
    this.backdrop = backdrop;
    this.items = Collections.unmodifiableList(new ArrayList<>(items));
  }

  @Override
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

  public List<T> getItems() {
    return items;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "['" + name + "' " + items + "]";
  }
}

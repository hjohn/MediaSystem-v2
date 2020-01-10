package hs.mediasystem.domain.work;

import hs.mediasystem.util.ImageURI;

import java.util.Optional;

public class Collection {
  private final String title;
  private final Optional<ImageURI> image;
  private final Optional<ImageURI> backdrop;
  private final CollectionDefinition definition;

  public Collection(String title, ImageURI image, ImageURI backdrop, CollectionDefinition definition) {
    this.title = title;
    this.image = Optional.ofNullable(image);
    this.backdrop = Optional.ofNullable(backdrop);
    this.definition = definition;
  }

  public String getTitle() {
    return title;
  }

  public Optional<ImageURI> getImage() {
    return image;
  }

  public Optional<ImageURI> getBackdrop() {
    return backdrop;
  }

  public CollectionDefinition getDefinition() {
    return definition;
  }
}

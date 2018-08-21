package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

public class Chronology {
  private final Identifier identifier;
  private final String name;
  private final ImageURI image;
  private final ImageURI backdrop;

  public Chronology(Identifier identifier, String name, ImageURI image, ImageURI backdrop) {
    this.identifier = identifier;
    this.name = name;
    this.image = image;
    this.backdrop = backdrop;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public String getName() {
    return name;
  }

  public ImageURI getImage() {
    return image;
  }

  public ImageURI getBackdrop() {
    return backdrop;
  }
}

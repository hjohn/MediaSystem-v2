package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

public class Person {
  private final PersonIdentifier identifier;
  private final String name;
  private final ImageURI image;

  public Person(PersonIdentifier identifier, String name, ImageURI image) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    this.identifier = identifier;
    this.name = name;
    this.image = image;
  }

  public PersonIdentifier getIdentifier() {
    return identifier;
  }

  public String getName() {
    return name;
  }

  public ImageURI getImage() {
    return image;
  }
}

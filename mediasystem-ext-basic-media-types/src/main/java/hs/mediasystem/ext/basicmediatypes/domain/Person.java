package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.util.ImageURI;

public class Person implements MediaDescriptor {
  private final PersonIdentifier identifier;
  private final String name;
  private final ImageURI cover;
  private final Details details;

  public Person(PersonIdentifier identifier, String name, ImageURI cover) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    this.identifier = identifier;
    this.name = name;
    this.details = new Details(name, null, null, null, cover, null, null);
    this.cover = cover;
  }

  @Override
  public PersonIdentifier getIdentifier() {
    return identifier;
  }

  @Override
  public Details getDetails() {
    return details;
  }

  public String getName() {
    return name;
  }

  public ImageURI getCover() {
    return cover;
  }
}

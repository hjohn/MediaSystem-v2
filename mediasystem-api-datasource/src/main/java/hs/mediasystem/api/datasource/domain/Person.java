package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.util.image.ImageURI;

public class Person {
  private final PersonId id;
  private final String name;
  private final ImageURI cover;
  private final Details details;

  public Person(PersonId id, String name, ImageURI cover) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    this.id = id;
    this.name = name;
    this.details = new Details(name, null, null, null, cover, null, null);
    this.cover = cover;
  }

  public PersonId getId() {
    return id;
  }

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

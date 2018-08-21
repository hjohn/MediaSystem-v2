package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;

public class Details {
  private final String name;
  private final String description;
  private final LocalDate date;
  private final ImageURI image;
  private final ImageURI backdrop;

  public Details(String name, String description, LocalDate date, ImageURI image, ImageURI backdrop) {
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    this.name = name;
    this.description = description;
    this.date = date;
    this.image = image;
    this.backdrop = backdrop;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getDate() {
    return date;
  }

  public ImageURI getImage() {
    return image;
  }

  public ImageURI getBackdrop() {
    return backdrop;
  }

  @Override
  public String toString() {
    return "Details[\"" + name + "\" @ " + date + "]";
  }
}

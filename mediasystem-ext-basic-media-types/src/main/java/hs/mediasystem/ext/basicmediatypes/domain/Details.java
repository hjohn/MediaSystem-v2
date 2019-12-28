package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.Optional;

public class Details {
  private final String name;
  private final Optional<String> description;
  private final Optional<LocalDate> date;
  private final Optional<ImageURI> image;
  private final Optional<ImageURI> backdrop;

  public Details(String name, String description, LocalDate date, ImageURI image, ImageURI backdrop) {
    if(name == null || name.isBlank()) {
      throw new IllegalArgumentException("name cannot be null or blank: " + name);
    }

    this.name = name;
    this.description = Optional.ofNullable(description);
    this.date = Optional.ofNullable(date);
    this.image = Optional.ofNullable(image);
    this.backdrop = Optional.ofNullable(backdrop);
  }

  public String getName() {
    return name;
  }

  public Optional<String> getDescription() {
    return description;
  }

  public Optional<LocalDate> getDate() {
    return date;
  }

  public Optional<ImageURI> getImage() {
    return image;
  }

  public Optional<ImageURI> getBackdrop() {
    return backdrop;
  }

  @Override
  public String toString() {
    return "Details[\"" + name + "\" @ " + date.orElse(null) + "]";
  }
}

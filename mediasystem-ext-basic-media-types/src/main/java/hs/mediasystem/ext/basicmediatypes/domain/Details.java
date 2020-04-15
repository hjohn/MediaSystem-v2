package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.Optional;

public class Details {
  private final String title;
  private final Optional<String> subtitle;
  private final Optional<String> description;
  private final Optional<LocalDate> date;
  private final Optional<ImageURI> image;
  private final Optional<ImageURI> backdrop;

  public Details(String title, String subtitle, String description, LocalDate date, ImageURI image, ImageURI backdrop) {
    if(title == null || title.isBlank()) {
      throw new IllegalArgumentException("title cannot be null or blank: " + title);
    }

    this.title = title;
    this.subtitle = Optional.ofNullable(subtitle);
    this.description = Optional.ofNullable(description);
    this.date = Optional.ofNullable(date);
    this.image = Optional.ofNullable(image);
    this.backdrop = Optional.ofNullable(backdrop);
  }

  public String getTitle() {
    return title;
  }

  public Optional<String> getSubtitle() {
    return subtitle;
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
    return "Details[\"" + title + "\" @ " + date.orElse(null) + "]";
  }
}

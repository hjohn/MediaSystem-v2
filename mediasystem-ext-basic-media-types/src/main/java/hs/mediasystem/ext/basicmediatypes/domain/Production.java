package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents either a Movie, a TV Movie or an entire TV Series.
 */
public class Production {
  private final ProductionIdentifier identifier;
  private final String name;
  private final String description;
  private final LocalDate date;
  private final ImageURI image;
  private final ImageURI backdrop;
  private final Reception reception;
  private final Duration runtime;
  private final List<String> languages;
  private final List<String> genres;

  public Production(ProductionIdentifier identifier, String name, @Nullable String description, @Nullable LocalDate date, @Nullable ImageURI image, @Nullable ImageURI backdrop, @Nullable Reception reception, Duration runtime, List<String> languages, List<String> genres) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
//    if(date == null) {
//      throw new IllegalArgumentException("date cannot be null");
//    }

    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.date = date;
    this.image = image;
    this.backdrop = backdrop;
    this.reception = reception;
    this.runtime = runtime;
    this.languages = languages;
    this.genres = genres;
  }

  public ProductionIdentifier getIdentifier() {
    return identifier;
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

  public Reception getReception() {
    return reception;
  }

  public Duration getRuntime() {
    return runtime;
  }

  public List<String> getLanguages() {
    return languages;
  }

  public List<String> getGenres() {
    return genres;
  }
}

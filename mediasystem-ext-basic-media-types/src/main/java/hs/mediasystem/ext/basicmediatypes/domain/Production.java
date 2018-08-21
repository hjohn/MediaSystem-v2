package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.ext.basicmediatypes.scan.MediaDescriptor;
import hs.mediasystem.util.ImageURI;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents either a Movie, a TV Movie or an entire TV Series.
 */
public class Production implements MediaDescriptor {
  private final ProductionIdentifier identifier;
  private final Details details;
  private final Reception reception;
  private final Duration runtime;
  private final List<String> languages;
  private final List<String> genres;

  public Production(ProductionIdentifier identifier, Details details, Reception reception, Duration runtime, List<String> languages, List<String> genres) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }

    this.identifier = identifier;
    this.details = details;
    this.reception = reception;
    this.runtime = runtime;
    this.languages = languages;
    this.genres = genres;
  }

  public ProductionIdentifier getIdentifier() {
    return identifier;
  }

  public String getName() {
    return details.getName();
  }

  public String getDescription() {
    return details.getDescription();
  }

  public LocalDate getDate() {
    return details.getDate();
  }

  public ImageURI getImage() {
    return details.getImage();
  }

  public ImageURI getBackdrop() {
    return details.getBackdrop();
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

  @Override
  public String toString() {
    return "Production[" + identifier + ": " + details + "]";
  }
}

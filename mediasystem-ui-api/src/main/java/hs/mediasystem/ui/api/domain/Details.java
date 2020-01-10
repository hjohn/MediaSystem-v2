package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.Optional;

public class Details {
  private final String name;
  private final Optional<String> description;
  private final Optional<LocalDate> releaseDate;
  private final Optional<LocalDate> lastAirDate;
  private final Optional<ImageURI> image;
  private final Optional<ImageURI> backdrop;
  private final Optional<String> tagline;
  private final Optional<Sequence> sequence;
  private final Optional<Reception> reception;
  private final Optional<Double> popularity;
  private final Classification classification;

  public Details(String name, String description, LocalDate releaseDate, LocalDate lastAirDate, ImageURI image, ImageURI backdrop, String tagline, Sequence sequence, Reception reception, Double popularity, Classification classification) {
    if(name == null || name.isBlank()) {
      throw new IllegalArgumentException("name cannot be null or blank: " + name);
    }
    if(classification == null) {
      throw new IllegalArgumentException("classification cannot be null");
    }

    this.name = name;
    this.description = Optional.ofNullable(description);
    this.releaseDate = Optional.ofNullable(releaseDate);
    this.lastAirDate = Optional.ofNullable(lastAirDate);
    this.image = Optional.ofNullable(image);
    this.backdrop = Optional.ofNullable(backdrop);
    this.tagline = Optional.ofNullable(tagline);
    this.sequence = Optional.ofNullable(sequence);
    this.reception = Optional.ofNullable(reception);
    this.popularity = Optional.ofNullable(popularity);
    this.classification = classification;
  }

  public String getName() {
    return name;
  }

  public Optional<String> getDescription() {
    return description;
  }

  public Optional<LocalDate> getReleaseDate() {
    return releaseDate;
  }

  public Optional<LocalDate> getLastAirDate() {
    return lastAirDate;
  }

  public Optional<ImageURI> getImage() {
    return image;
  }

  public Optional<ImageURI> getBackdrop() {
    return backdrop;
  }

  public Optional<String> getTagline() {
    return tagline;
  }

  public Optional<Sequence> getSequence() {
    return sequence;
  }

  public Optional<Reception> getReception() {
    return reception;
  }

  public Optional<Double> getPopularity() {
    return popularity;
  }

  public Classification getClassification() {
    return classification;
  }
}

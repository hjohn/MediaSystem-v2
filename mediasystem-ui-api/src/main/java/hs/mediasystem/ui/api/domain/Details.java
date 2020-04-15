package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.Optional;

public class Details {
  private final String title;
  private final Optional<String> subtitle;
  private final Optional<String> description;
  private final Optional<LocalDate> releaseDate;
  private final Optional<ImageURI> image;
  private final Optional<ImageURI> backdrop;
  private final Optional<String> tagline;
  private final Optional<Serie> serie;
  private final Optional<Sequence> sequence;
  private final Optional<Reception> reception;
  private final Optional<Double> popularity;
  private final Classification classification;

  public Details(String title, String subtitle, String description, LocalDate releaseDate, ImageURI image, ImageURI backdrop, String tagline, Serie serie, Sequence sequence, Reception reception, Double popularity, Classification classification) {
    if(title == null || title.isBlank()) {
      throw new IllegalArgumentException("title cannot be null or blank: " + title);
    }
    if(classification == null) {
      throw new IllegalArgumentException("classification cannot be null");
    }

    this.title = title;
    this.subtitle = Optional.ofNullable(subtitle);
    this.description = Optional.ofNullable(description);
    this.releaseDate = Optional.ofNullable(releaseDate);
    this.image = Optional.ofNullable(image);
    this.backdrop = Optional.ofNullable(backdrop);
    this.tagline = Optional.ofNullable(tagline);
    this.serie = Optional.ofNullable(serie);
    this.sequence = Optional.ofNullable(sequence);
    this.reception = Optional.ofNullable(reception);
    this.popularity = Optional.ofNullable(popularity);
    this.classification = classification;
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

  public Optional<LocalDate> getReleaseDate() {
    return releaseDate;
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

  public Optional<Serie> getSerie() {
    return serie;
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

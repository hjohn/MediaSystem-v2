package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

public class Details {
  public static final Comparator<Details> ALPHABETICAL = Comparator.comparing(Details::getTitle, NaturalLanguage.ALPHABETICAL);
  public static final Comparator<Details> RELEASE_DATE = Comparator.comparing(d -> d.getReleaseDate().orElse(null), Comparator.nullsLast(Comparator.naturalOrder()));
  public static final Comparator<Details> RELEASE_DATE_REVERSED = Comparator.comparing((Details d) -> d.getReleaseDate().orElse(null), Comparator.nullsFirst(Comparator.naturalOrder())).reversed();

  private final String title;
  private final Optional<String> subtitle;
  private final Optional<String> description;
  private final Optional<LocalDate> releaseDate;
  private final Optional<ImageURI> cover;
  private final Optional<ImageURI> sampleImage;
  private final Optional<ImageURI> backdrop;
  private final Optional<String> tagline;
  private final Optional<Serie> serie;
  private final Optional<Sequence> sequence;
  private final Optional<Reception> reception;
  private final Optional<Double> popularity;
  private final Classification classification;

  public Details(String title, String subtitle, String description, LocalDate releaseDate, ImageURI cover, ImageURI sampleImage, ImageURI backdrop, String tagline, Serie serie, Sequence sequence, Reception reception, Double popularity, Classification classification) {
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
    this.cover = Optional.ofNullable(cover);
    this.sampleImage = Optional.ofNullable(sampleImage);
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

  /**
   * Returns a cover image, always with aspect ratio 3:2.  If present,
   * this will be the most descriptive image, potentially containing
   * identifying text as part of the image.
   *
   * @return a cover image
   */
  public Optional<ImageURI> getCover() {
    return cover;
  }

  /**
   * Returns a sample image from the underlying media (if any).  This
   * image can either be provided by a 3rd party service or be an
   * extracted image from the media.  The aspect ratio can vary but
   * generally will be 2.39:1, 1.85:1, 16:9 or 4:3 or thereabouts.
   *
   * @return a sample image
   */
  public Optional<ImageURI> getSampleImage() {
    return sampleImage;
  }

  /**
   * Returns a suitable backdrop image, always with an aspect ratio
   * of 16:9.  The backdrop image generally does not contain any
   * textual markings and is therefore language neutral.
   *
   * @return a backdrop image
   */
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

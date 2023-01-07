package hs.mediasystem.ui.api.domain;

import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.natural.NaturalLanguage;

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
  private final Optional<ImageHandle> cover;
  private final Optional<ImageHandle> autoCover;
  private final Optional<ImageHandle> sampleImage;
  private final Optional<ImageHandle> backdrop;
  private final Optional<String> tagline;
  private final Optional<Serie> serie;
  private final Optional<Sequence> sequence;
  private final Optional<Reception> reception;
  private final Optional<Double> popularity;
  private final Classification classification;

  public Details(String title, String subtitle, String description, LocalDate releaseDate, ImageHandle cover, ImageHandle autoCover, ImageHandle sampleImage, ImageHandle backdrop, String tagline, Serie serie, Sequence sequence, Reception reception, Double popularity, Classification classification) {
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
    this.autoCover = Optional.ofNullable(autoCover);
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
  public Optional<ImageHandle> getCover() {
    return cover;
  }

  /**
   * Returns a cover image, always with aspect ratio 3:2.  If present,
   * this image is generated from several snapshots of the media.
   *
   * @return a cover image
   */
  public Optional<ImageHandle> getAutoCover() {
    return autoCover;
  }

  /**
   * Returns the cover if present otherwise the automatically generated
   * cover if present.
   *
   * @return a cover image
   */
  public Optional<ImageHandle> getAnyCover() {
    return getCover().or(this::getAutoCover);
  }

  /**
   * Returns a sample image from the underlying media (if any).  This
   * image can either be provided by a 3rd party service or be an
   * extracted image from the media.  The aspect ratio can vary but
   * generally will be 2.39:1, 1.85:1, 16:9 or 4:3 or thereabouts.
   *
   * @return a sample image
   */
  public Optional<ImageHandle> getSampleImage() {
    return sampleImage;
  }

  /**
   * Returns a suitable backdrop image, always with an aspect ratio
   * of 16:9.  The backdrop image generally does not contain any
   * textual markings and is therefore language neutral.
   *
   * @return a backdrop image
   */
  public Optional<ImageHandle> getBackdrop() {
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

  public String getYearRange() {
    LocalDate date = getReleaseDate().orElse(null);

    if(date == null) {
      return "Unknown";
    }

    Stage stage = getClassification().stage().orElse(null);
    LocalDate lastAirDate = getSerie().flatMap(Serie::lastAirDate).orElse(null);

    if(stage == Stage.ENDED && lastAirDate != null && lastAirDate.getYear() != date.getYear()) {
      return date.getYear() + " - " + lastAirDate.getYear();
    }
    else if(getSerie().isPresent() && stage == Stage.RELEASED) {
      return date.getYear() + " -";
    }

    return "" + date.getYear();
  }
}

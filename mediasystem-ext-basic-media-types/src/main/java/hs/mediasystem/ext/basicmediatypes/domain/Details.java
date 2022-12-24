package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.util.ImageURI;

import java.time.LocalDate;
import java.util.Optional;

public class Details {
  private final String title;
  private final Optional<String> subtitle;
  private final Optional<String> description;
  private final Optional<LocalDate> date;
  private final Optional<ImageURI> cover;
  private final Optional<ImageURI> sampleImage;
  private final Optional<ImageURI> backdrop;

  public Details(String title, String subtitle, String description, LocalDate date, ImageURI cover, ImageURI sampleImage, ImageURI backdrop) {
    if(title == null || title.isBlank()) {
      throw new IllegalArgumentException("title cannot be null or blank: " + title);
    }

    this.title = title;
    this.subtitle = Optional.ofNullable(subtitle);
    this.description = Optional.ofNullable(description);
    this.date = Optional.ofNullable(date);
    this.cover = Optional.ofNullable(cover);
    this.sampleImage = Optional.ofNullable(sampleImage);
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

  @Override
  public String toString() {
    return "Details[\"" + title + "\" @ " + date.orElse(null) + (getCover().isPresent() ? "[cover]" : "") + (getSampleImage().isPresent() ? "[sample]" : "") + (getBackdrop().isPresent() ? "[backdrop]" : "") + "]";
  }
}

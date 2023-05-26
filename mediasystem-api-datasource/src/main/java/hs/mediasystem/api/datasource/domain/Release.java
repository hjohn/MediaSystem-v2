package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.domain.work.Context;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.image.ImageURI;

import java.time.LocalDate;
import java.util.Optional;

public class Release implements WorkDescriptor {
  private final WorkId id;
  private final Details details;
  private final Reception reception;
  private final Optional<Context> context;

  public Release(WorkId id, Details details, Reception reception, Context context) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }

    this.id = id;
    this.details = details;
    this.reception = reception;
    this.context = Optional.ofNullable(context);
  }

  @Override
  public WorkId getId() {
    return id;
  }

  @Override
  public Details getDetails() {
    return details;
  }

  public Optional<Context> getContext() {
    return context;
  }

  public String getTitle() {
    return details.getTitle();
  }

  public Optional<String> getDescription() {
    return details.getDescription();
  }

  public Optional<LocalDate> getDate() {
    return details.getDate();
  }

  /**
   * Returns a cover image, always with aspect ratio 3:2.  If present,
   * this will be the most descriptive image, potentially containing
   * identifying text as part of the image.
   *
   * @return a cover image
   */
  public Optional<ImageURI> getCover() {
    return details.getCover();
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
    return details.getSampleImage();
  }

  /**
   * Returns a suitable backdrop image, always with an aspect ratio
   * of 16:9.  The backdrop image generally does not contain any
   * textual markings and is therefore language neutral.
   *
   * @return a backdrop image
   */
  public Optional<ImageURI> getBackdrop() {
    return details.getBackdrop();
  }

  public Reception getReception() {
    return reception;
  }

  @Override
  public String toString() {
    return "Release[" + id + ": " + details + "]";
  }
}

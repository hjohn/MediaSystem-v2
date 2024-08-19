package hs.mediasystem.api.discovery;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.domain.URIs;

import java.net.URI;
import java.time.Instant;

/**
 * A discovered location with a media.
 *
 * @param mediaType a {@link MediaType}, cannot be {@code null}
 * @param location a {@link URI}, cannot be {@code null}
 * @param attributes an {@link Attributes}, cannot be {@code null}
 * @param lastModificationTime an {@link Instant}, cannot be {@code null}
 * @param size a size, can be {@code null} but cannot be negative
 */
public record Discovery(MediaType mediaType, URI location, Attributes attributes, Instant lastModificationTime, Long size) {

  public Discovery {
    if(mediaType == null) {
      throw new IllegalArgumentException("mediaType cannot be null");
    }
    if(location == null) {
      throw new IllegalArgumentException("location cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(lastModificationTime == null) {
      throw new IllegalArgumentException("lastModificationTime cannot be null");
    }
    if(size != null && size < 0) {
      throw new IllegalArgumentException("size cannot be negative: " + size);
    }

    location = URIs.normalizeAsFile(location);
  }
}

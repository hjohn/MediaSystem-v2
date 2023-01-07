package hs.mediasystem.ext.basicmediatypes.api;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.domain.URIs;

import java.net.URI;
import java.util.Optional;

/**
 * A discovered location with a media.
 *
 * @param mediaType a {@link MediaType}, cannot be {@code null}
 * @param location a {@link URI}, cannot be {@code null}
 * @param attributes an {@link Attributes}, cannot be {@code null}
 * @param parentLocation an optional parent location (required for component media types), cannot be {@code null}
 * @param contentPrint a {@link ContentPrint}, cannot be {@code null}
 */
public record Discovery(MediaType mediaType, URI location, Attributes attributes, Optional<URI> parentLocation, ContentPrint contentPrint) {

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
    if(parentLocation == null) {
      throw new IllegalArgumentException("parentLocation cannot be null");
    }
    if(contentPrint == null) {
      throw new IllegalArgumentException("contentPrint cannot be null");
    }

    if(mediaType.isComponent() && parentLocation.isEmpty()) {
      throw new IllegalArgumentException("parentLocation must be provided for Component MediaTypes: " + parentLocation);
    }

    location = URIs.normalizeAsFile(location);
    parentLocation = parentLocation.map(URIs::normalizeAsFile);
  }
}

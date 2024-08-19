package hs.mediasystem.db.core;

import hs.mediasystem.db.services.domain.ContentPrint;
import hs.mediasystem.domain.media.StreamDescriptor;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;

import java.net.URI;
import java.util.Optional;

public record Streamable(MediaType mediaType, URI location, ContentPrint contentPrint, Optional<URI> parentLocation, StreamTags tags, Optional<StreamDescriptor> descriptor) {

  public Streamable {
    if(mediaType == null) {
      throw new IllegalArgumentException("mediaType cannot be null");
    }
    if(location == null) {
      throw new IllegalArgumentException("location cannot be null");
    }
    if(contentPrint == null) {
      throw new IllegalArgumentException("contentPrint cannot be null");
    }
    if(parentLocation == null) {
      throw new IllegalArgumentException("parentLocation cannot be null");
    }
    if(tags == null) {
      throw new IllegalArgumentException("tags cannot be null");
    }
    if(descriptor == null) {
      throw new IllegalArgumentException("descriptor cannot be null");
    }
  }

  public ContentID contentId() {
    return contentPrint().getId();
  }

  public Streamable with(StreamDescriptor descriptor) {
    return new Streamable(
      mediaType,
      location,
      contentPrint,
      parentLocation,
      tags,
      Optional.of(descriptor)
    );
  }
}

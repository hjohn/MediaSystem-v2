package hs.mediasystem.db.services.domain;

import hs.mediasystem.db.core.StreamTags;
import hs.mediasystem.domain.media.MediaStructure;
import hs.mediasystem.domain.media.Snapshot;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record Resource(
  URI location,
  Optional<URI> parentLocation,
  MediaType mediaType,
  ContentID contentId,
  Instant lastModificationTime,
  Optional<Long> size,
  Instant discoveryTime,
  StreamTags tags,
  Optional<Duration> duration,
  Optional<MediaStructure> mediaStructure,
  List<Snapshot> snapshots,
  Attributes attributes
) {

  public Resource {
    if(location == null) {
      throw new IllegalArgumentException("location cannot be null");
    }
    if(parentLocation == null) {
      throw new IllegalArgumentException("parentLocation cannot be null");
    }
    if(mediaType == null) {
      throw new IllegalArgumentException("mediaType cannot be null");
    }
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
    }
    if(lastModificationTime == null) {
      throw new IllegalArgumentException("lastModificationTime cannot be null");
    }
    if(size == null) {
      throw new IllegalArgumentException("size cannot be null");
    }
    if(size.filter(s -> s < 0).isPresent()) {
      throw new IllegalArgumentException("size cannot be negative: " + size);
    }
    if(discoveryTime == null) {
      throw new IllegalArgumentException("discoveryTime cannot be null");
    }
    if(tags == null) {
      throw new IllegalArgumentException("tags cannot be null");
    }
    if(duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }
    if(mediaStructure == null) {
      throw new IllegalArgumentException("mediaStructure cannot be null");
    }
    if(snapshots == null) {
      throw new IllegalArgumentException("snapshots cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }

    if(mediaType.parent().isPresent() && parentLocation.isEmpty()) {
      throw new IllegalArgumentException("parentLocation must be present when media type is: " + mediaType);
    }
  }
}

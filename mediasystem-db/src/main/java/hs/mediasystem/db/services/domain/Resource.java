package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.MediaStructure;
import hs.mediasystem.domain.work.Snapshot;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.mediamanager.StreamTags;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record Resource(
  StreamID id,
  Optional<StreamID> parentId,
  URI uri,
  Attributes attributes,
  Instant discoveryTime,
  Instant lastModificationTime,
  Optional<Long> size,
  StreamTags tags,
  Optional<Duration> duration,
  Optional<MediaStructure> mediaStructure,
  List<Snapshot> snapshots
) {
  public Resource {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(parentId == null) {
      throw new IllegalArgumentException("parentId cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }
    if(discoveryTime == null) {
      throw new IllegalArgumentException("discoveryTime cannot be null");
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
  }
}

package hs.mediasystem.domain.work;

import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

public class StreamAttributes {
  private final URI uri;
  private final Instant creationTime;  // This is time it was first added to database
  private final Instant lastModificationTime;
  private final Optional<Long> size;
  private final Attributes attributes;

  public StreamAttributes(URI uri, Instant creationTime, Instant lastModificationTime, Long size, Attributes attributes) {
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(creationTime == null) {
      throw new IllegalArgumentException("creationTime cannot be null");
    }
    if(lastModificationTime == null) {
      throw new IllegalArgumentException("lastModifiedTime cannot be null");
    }
    if(size != null && size < 0) {
      throw new IllegalArgumentException("size cannot be negative: " + size);
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }

    this.uri = uri;
    this.creationTime = creationTime;
    this.lastModificationTime = lastModificationTime;
    this.size = Optional.ofNullable(size);
    this.attributes = attributes;
  }

  public URI getUri() {
    return uri;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public Instant getLastModificationTime() {
    return lastModificationTime;
  }

  public Optional<Long> getSize() {
    return size;
  }

  public Attributes getAttributes() {
    return attributes;
  }
}

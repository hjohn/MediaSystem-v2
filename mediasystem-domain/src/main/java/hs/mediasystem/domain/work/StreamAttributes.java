package hs.mediasystem.domain.work;

import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

public class StreamAttributes {
  private final URI uri;
  private final Instant discoveryTime;
  private final Instant lastModificationTime;
  private final Optional<Long> size;
  private final Attributes attributes;

  public StreamAttributes(URI uri, Instant discoveryTime, Instant lastModificationTime, Long size, Attributes attributes) {
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(discoveryTime == null) {
      throw new IllegalArgumentException("discoveryTime cannot be null");
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
    this.discoveryTime = discoveryTime;
    this.lastModificationTime = lastModificationTime;
    this.size = Optional.ofNullable(size);
    this.attributes = attributes;
  }

  public URI getUri() {
    return uri;
  }

  /**
   * Returns the time the item was first discovered.
   *
   * @return the time the item was first discovered, never <code>null</code>
   */
  public Instant getDiscoveryTime() {
    return discoveryTime;
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

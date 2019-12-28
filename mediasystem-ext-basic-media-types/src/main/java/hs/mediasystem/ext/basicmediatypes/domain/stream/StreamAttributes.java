package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.time.Instant;

public class StreamAttributes {
  private final StringURI uri;
  private final Instant creationTime;
  private final Attributes attributes;

  public StreamAttributes(StringURI uri, Instant creationTime, Attributes attributes) {
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(creationTime == null) {
      throw new IllegalArgumentException("creationTime cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }

    this.uri = uri;
    this.creationTime = creationTime;
    this.attributes = attributes;
  }

  public StringURI getUri() {
    return uri;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public Attributes getAttributes() {
    return attributes;
  }
}

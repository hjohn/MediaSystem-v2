package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.scanner.api.Attribute;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

public class StreamAttributes {
  private final MediaType type;
  private final StringURI uri;
  private final Attributes attributes;

  public StreamAttributes(MediaType type, StringURI uri, Attributes attributes) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
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

    this.type = type;
    this.uri = uri;
    this.attributes = attributes;
  }

  public MediaType getType() {
    return type;
  }

  public StringURI getUri() {
    return uri;
  }

  public Attributes getAttributes() {
    return attributes;
  }
}

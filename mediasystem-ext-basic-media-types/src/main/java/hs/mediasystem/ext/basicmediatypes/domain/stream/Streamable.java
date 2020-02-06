package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Objects;
import java.util.Optional;

public class Streamable {
  private final MediaType type;
  private final StringURI uri;
  private final StreamID streamId;
  private final Optional<StreamID> parentStreamId;
  private final Attributes attributes;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param uri a {@link StringURI}, cannot be null
   * @param streamId a {@link StreamID}, cannot be null
   * @param parentStreamId a parent {@link StreamID}, can be null
   * @param attributes an {@link Attributes}, cannot be null
   */
  public Streamable(MediaType type, StringURI uri, StreamID streamId, StreamID parentStreamId, Attributes attributes) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(streamId == null) {
      throw new IllegalArgumentException("streamId cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }

    this.type = type;
    this.uri = uri;
    this.streamId = streamId;
    this.parentStreamId = Optional.ofNullable(parentStreamId);
    this.attributes = attributes;
  }

  public StringURI getUri() {
    return uri;
  }

  public MediaType getType() {
    return type;
  }

  public StreamID getId() {
    return streamId;
  }

  public Optional<StreamID> getParentStreamId() {
    return parentStreamId;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, parentStreamId, streamId, type, uri);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Streamable other = (Streamable)obj;

    return Objects.equals(attributes, other.attributes)
        && Objects.equals(parentStreamId, other.parentStreamId)
        && Objects.equals(streamId, other.streamId)
        && Objects.equals(type, other.type)
        && Objects.equals(uri, other.uri);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + uri.toURI().getPath() + "]";
  }
}

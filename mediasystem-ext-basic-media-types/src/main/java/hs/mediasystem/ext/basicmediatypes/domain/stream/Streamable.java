package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class Streamable {
  private final MediaType type;
  private final URI uri;
  private final StreamID id;
  private final Optional<StreamID> parentId;
  private final Attributes attributes;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param uri a {@link URI}, cannot be null
   * @param id a {@link StreamID}, cannot be null
   * @param parentId a parent {@link StreamID}, can be null
   * @param attributes an {@link Attributes}, cannot be null
   */
  public Streamable(MediaType type, URI uri, StreamID id, StreamID parentId, Attributes attributes) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }

    this.type = type;
    this.uri = uri;
    this.id = id;
    this.parentId = Optional.ofNullable(parentId);
    this.attributes = attributes;
  }

  public String getName() {
    String name = uri.toString();

    return name.substring(name.lastIndexOf('/') + 1);
  }

  public URI getUri() {
    return uri;
  }

  public MediaType getType() {
    return type;
  }

  public StreamID getId() {
    return id;
  }

  public Optional<StreamID> getParentId() {
    return parentId;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, parentId, id, type, uri);
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
        && Objects.equals(parentId, other.parentId)
        && Objects.equals(id, other.id)
        && Objects.equals(type, other.type)
        && Objects.equals(uri, other.uri);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + uri.getPath() + "]";
  }
}

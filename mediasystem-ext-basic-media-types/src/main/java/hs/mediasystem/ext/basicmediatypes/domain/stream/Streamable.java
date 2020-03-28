package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Objects;
import java.util.Optional;

public class Streamable {
  private final MediaType type;
  private final StringURI uri;
  private final ContentID contentId;
  private final Optional<ContentID> parentContentId;
  private final Attributes attributes;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param uri a {@link StringURI}, cannot be null
   * @param contentId a {@link ContentID}, cannot be null
   * @param parentContentId a parent {@link ContentID}, can be null
   * @param attributes an {@link Attributes}, cannot be null
   */
  public Streamable(MediaType type, StringURI uri, ContentID contentId, ContentID parentContentId, Attributes attributes) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(!attributes.contains(Attribute.TITLE)) {
      throw new IllegalArgumentException("attributes must contain Attribute.TITLE");
    }

    this.type = type;
    this.uri = uri;
    this.contentId = contentId;
    this.parentContentId = Optional.ofNullable(parentContentId);
    this.attributes = attributes;
  }

  public StringURI getUri() {
    return uri;
  }

  public MediaType getType() {
    return type;
  }

  public ContentID getId() {
    return contentId;
  }

  public Optional<ContentID> getParentContentId() {
    return parentContentId;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, parentContentId, contentId, type, uri);
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
        && Objects.equals(parentContentId, other.parentContentId)
        && Objects.equals(contentId, other.contentId)
        && Objects.equals(type, other.type)
        && Objects.equals(uri, other.uri);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + uri.toURI().getPath() + "]";
  }
}

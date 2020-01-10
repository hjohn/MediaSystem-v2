package hs.mediasystem.domain.stream;

import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BasicStream {
  private final MediaType type;
  private final StringURI uri;
  private final StreamID streamId;
  private final Attributes attributes;
  private final List<BasicStream> children;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param uri a {@link StringURI}, cannot be null
   * @param streamId a {@link StreamID}, cannot be null
   * @param attributes an {@link Attributes}, cannot be null
   * @param children a {@link List} of child streams, cannot be null but can be empty
   */
  public BasicStream(MediaType type, StringURI uri, StreamID streamId, Attributes attributes, List<BasicStream> children) {
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
    if(children == null) {
      throw new IllegalArgumentException("children cannot be null");
    }

    this.type = type;
    this.uri = uri;
    this.streamId = streamId;
    this.attributes = attributes;
    this.children = Collections.unmodifiableList(children.stream()
      .sorted(Comparator.comparing(BasicStream::getUri))  // Sort so that equals/hashcode is stable
      .filter(Objects::nonNull)
      .collect(Collectors.toList())
    );

    if(this.children.size() != children.size()) {
      throw new IllegalArgumentException("children cannot contain nulls");
    }
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

  public Attributes getAttributes() {
    return attributes;
  }

  public List<BasicStream> getChildren() {
    return children;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, streamId, type, children);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    BasicStream other = (BasicStream)obj;

    return Objects.equals(attributes, other.attributes)
      && Objects.equals(streamId, other.streamId)
      && Objects.equals(type, other.type)
      && Objects.equals(children, other.children);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + uri.toURI().getPath() + "]";
  }
}

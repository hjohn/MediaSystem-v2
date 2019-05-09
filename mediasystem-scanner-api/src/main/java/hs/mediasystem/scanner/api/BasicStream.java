package hs.mediasystem.scanner.api;

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
  private final StreamPrint streamPrint;
  private final Attributes attributes;
  private final List<BasicStream> children;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param uri a {@link StringURI}, cannot be null
   * @param streamPrint a {@link StreamPrint}, cannot be null
   * @param attributes an {@link Attributes}, cannot be null
   * @param children a {@link List} of child streams, cannot be null but can be empty
   */
  public BasicStream(MediaType type, StringURI uri, StreamPrint streamPrint, Attributes attributes, List<BasicStream> children) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(streamPrint == null) {
      throw new IllegalArgumentException("streamPrint cannot be null");
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
    this.streamPrint = streamPrint;
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
    return streamPrint.getId();
  }

  public StreamPrint getStreamPrint() {
    return streamPrint;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public List<BasicStream> getChildren() {
    return children;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributes, streamPrint, type, children);
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
      && Objects.equals(streamPrint, other.streamPrint)
      && Objects.equals(type, other.type)
      && Objects.equals(children, other.children);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + uri.toURI().getPath() + "]";
  }
}

package hs.mediasystem.ext.basicmediatypes.scan;

import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Type;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractMediaStream<D extends MediaDescriptor> implements MediaStream<D> {
  private final StreamPrint streamPrint;
  private final Attributes attributes;
  private final StringURI parentUri;

  private final transient Type type;

  private volatile Map<Identifier, MediaRecord<D>> mediaRecords;  // This field is updated when a MediaRecord is merged (which can happen from other threads)

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param parentUri a parent URI, can be null
   * @param streamPrint a {@link StreamPrint}, cannot be null
   * @param attributes an {@link Attributes}, cannot be null
   * @param mediaRecords a set of media records, can be empty, cannot contain null keys or values
   */
  public AbstractMediaStream(Type type, StringURI parentUri, StreamPrint streamPrint, Attributes attributes, Map<Identifier, MediaRecord<D>> mediaRecords) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(streamPrint == null) {
      throw new IllegalArgumentException("streamPrint cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(mediaRecords == null) {
      throw new IllegalArgumentException("mediaRecords cannot be null");
    }
    if(mediaRecords.containsKey(null)) {
      throw new IllegalArgumentException("mediaRecords cannot contain null keys: " + mediaRecords);
    }
    if(mediaRecords.containsValue(null)) {
      throw new IllegalArgumentException("mediaRecords cannot contain null values: " + mediaRecords);
    }

    this.type = type;
    this.parentUri = parentUri;
    this.streamPrint = streamPrint;
    this.attributes = attributes;
    this.mediaRecords = Collections.unmodifiableMap(new HashMap<>(mediaRecords));
  }

  @Override
  public StringURI getParentUri() {
    return parentUri;
  }

  @Override
  public StringURI getUri() {
    return streamPrint.getUri();
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public StreamPrint getStreamPrint() {
    return streamPrint;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public Map<Identifier, MediaRecord<D>> getMediaRecords() {
    return mediaRecords;
  }

  @Override
  public synchronized void mergeMediaRecord(MediaRecord<D> mediaRecord) {
    if(mediaRecord == null) {
      throw new IllegalArgumentException("mediaRecord cannot be null");
    }

    Map<Identifier, MediaRecord<D>> map = new HashMap<>(mediaRecords);

    map.put(mediaRecord.getIdentification().getIdentifier(), mediaRecord);

    this.mediaRecords = Collections.unmodifiableMap(map);
  }

  @Override
  public synchronized void replaceMediaRecords(List<MediaRecord<D>> mediaRecords) {
    if(mediaRecords == null) {
      throw new IllegalArgumentException("mediaRecords cannot be null");
    }
    if(mediaRecords.contains(null)) {
      throw new IllegalArgumentException("mediaRecords cannot contain nulls: " + mediaRecords);
    }

    this.mediaRecords = Collections.unmodifiableMap(mediaRecords.stream().collect(Collectors.toMap(r -> r.getIdentification().getIdentifier(), Function.identity())));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getStreamPrint().getUri().toURI().getPath() + "]";
  }
}

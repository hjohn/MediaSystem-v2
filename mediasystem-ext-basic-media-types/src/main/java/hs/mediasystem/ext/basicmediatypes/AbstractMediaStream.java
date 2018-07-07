package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMediaStream<D extends MediaDescriptor> implements MediaStream<D> {
  private final StreamPrint streamPrint;
  private final Attributes attributes;
  private final StringURI parentUri;

  private final transient Type type;

  private volatile Map<Identifier, MediaRecord<D>> mediaRecords;  // This field is updated when a MediaRecord is merged (which can happen from other threads)

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

    this.type = type;
    this.parentUri = parentUri;
    this.streamPrint = streamPrint;
    this.attributes = attributes;
    this.mediaRecords = Collections.unmodifiableMap(mediaRecords == null ? new HashMap<>() : new HashMap<>(mediaRecords));
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
}

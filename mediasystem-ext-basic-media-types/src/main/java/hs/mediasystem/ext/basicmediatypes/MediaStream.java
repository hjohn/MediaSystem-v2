package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MediaStream {
  private final BasicStream stream;
  private final Map<Identifier, MediaRecord> mediaRecords;
  private final Instant lastEnrichTime;
  private final Instant nextEnrichTime;

  /**
   * Constructs a new instance.
   *
   * @param type a {@link MediaType}, cannot be null
   * @param parentUri a parent URI, can be null
   * @param streamPrint a {@link StreamPrint}, cannot be null
   * @param attributes an {@link Attributes}, cannot be null
   * @param mediaRecords a set of media records, can be empty, cannot contain null keys or values
   */
  public MediaStream(BasicStream stream, Instant lastEnrichTime, Instant nextEnrichTime, Map<Identifier, MediaRecord> mediaRecords) {
    if(stream == null) {
      throw new IllegalArgumentException("stream cannot be null");
    }
    if(mediaRecords == null) {
      throw new IllegalArgumentException("mediaRecords cannot be null");
    }

    this.stream = stream;
    this.lastEnrichTime = lastEnrichTime;
    this.nextEnrichTime = nextEnrichTime;
    this.mediaRecords = Collections.unmodifiableMap(new HashMap<>(mediaRecords));

    if(this.mediaRecords.containsKey(null)) {
      throw new IllegalArgumentException("mediaRecords cannot contain null keys: " + mediaRecords);
    }
    if(this.mediaRecords.containsValue(null)) {
      throw new IllegalArgumentException("mediaRecords cannot contain null values: " + mediaRecords);
    }
  }

  public BasicStream getStream() {
    return stream;
  }

  public StringURI getUri() {
    return stream.getUri();
  }

  public MediaType getType() {
    return stream.getType();
  }

  public StreamPrint getStreamPrint() {
    return stream.getStreamPrint();
  }

  public Attributes getAttributes() {
    return stream.getAttributes();
  }

  public Map<Identifier, MediaRecord> getMediaRecords() {
    return mediaRecords;
  }

  public Instant getLastEnrichTime() {
    return lastEnrichTime;
  }

  public Instant getNextEnrichTime() {
    return nextEnrichTime;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getUri().toURI().getPath() + "]";
  }
}

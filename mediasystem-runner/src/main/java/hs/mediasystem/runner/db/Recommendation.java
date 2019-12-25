package hs.mediasystem.runner.db;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class Recommendation {
  private final Instant instant;
  private final Optional<MediaDescriptor> parent;
  private final MediaDescriptor mediaDescriptor;
  private final Optional<Duration> length;
  private final Duration position;
  private final StreamID streamId;
  private final boolean watched;

  public Recommendation(Instant instant, MediaDescriptor parent, MediaDescriptor mediaDescriptor, StreamID streamId, Duration length, Duration position, boolean watched) {
    if(instant == null) {
      throw new IllegalArgumentException("instant cannot be null");
    }
    if(mediaDescriptor == null) {
      throw new IllegalArgumentException("mediaDescriptor cannot be null");
    }
    if(streamId == null) {
      throw new IllegalArgumentException("streamId cannot be null");
    }
    if(position == null || position.isNegative()) {
      throw new IllegalArgumentException("position cannot be null or negative: " + position);
    }

    this.instant = instant;
    this.parent = Optional.ofNullable(parent);
    this.mediaDescriptor = mediaDescriptor;
    this.streamId = streamId;
    this.length = Optional.ofNullable(length);
    this.position = position;
    this.watched = watched;
  }

  public Instant getInstant() {
    return instant;
  }

  public Optional<MediaDescriptor> getParent() {
    return parent;
  }

  public MediaDescriptor getMediaDescriptor() {
    return mediaDescriptor;
  }

  public Optional<Duration> getLength() {
    return length;
  }

  public Duration getPosition() {
    return position;
  }

  public StreamID getStreamId() {
    return streamId;
  }

  public boolean isWatched() {
    return watched;
  }
}
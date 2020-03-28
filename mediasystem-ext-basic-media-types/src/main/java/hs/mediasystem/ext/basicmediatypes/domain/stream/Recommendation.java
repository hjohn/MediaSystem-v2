package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class Recommendation {
  private final Instant instant;
  private final Work work;
  private final Optional<MediaDescriptor> parent;
  private final MediaDescriptor mediaDescriptor;
  private final Optional<Duration> length;
  private final Duration position;
  private final ContentID contentId;
  private final boolean watched;

  public Recommendation(Instant instant, Work work, MediaDescriptor parent, MediaDescriptor mediaDescriptor, ContentID contentId, Duration length, Duration position, boolean watched) {
    if(instant == null) {
      throw new IllegalArgumentException("instant cannot be null");
    }
    if(work == null) {
      throw new IllegalArgumentException("work cannot be null");
    }
    if(mediaDescriptor == null) {
      throw new IllegalArgumentException("mediaDescriptor cannot be null");
    }
    if(contentId == null) {
      throw new IllegalArgumentException("contentId cannot be null");
    }
    if(position == null || position.isNegative()) {
      throw new IllegalArgumentException("position cannot be null or negative: " + position);
    }

    this.instant = instant;
    this.work = work;
    this.parent = Optional.ofNullable(parent);
    this.mediaDescriptor = mediaDescriptor;
    this.contentId = contentId;
    this.length = Optional.ofNullable(length);
    this.position = position;
    this.watched = watched;
  }

  public Instant getInstant() {
    return instant;
  }

  public Work getWork() {
    return work;
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

  public ContentID getContentId() {
    return contentId;
  }

  public boolean isWatched() {
    return watched;
  }
}

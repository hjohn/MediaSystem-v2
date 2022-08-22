package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.util.Attributes;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class MediaStream {
  private final StreamID id;
  private final Optional<StreamID> parentId;
  private final URI uri;
  private final Instant discoveryTime;
  private final Instant lastModificationTime;
  private final Optional<Long> size;
  private final Attributes attributes;  // physical attributes
  private final State state;
  private final Optional<Duration> duration;
  private final Optional<MediaStructure> mediaStructure;  // logical attributes (tracks)
  private final List<Snapshot> snapshots;
  private final Match match;

  public MediaStream(StreamID id, StreamID parentId, URI uri, Instant discoveryTime, Instant lastModificationTime, Long size, Attributes attributes, State state, Duration duration, MediaStructure mediaStructure, List<Snapshot> snapshots, Match match) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(discoveryTime == null) {
      throw new IllegalArgumentException("discoveryTime cannot be null");
    }
    if(lastModificationTime == null) {
      throw new IllegalArgumentException("lastModifiedTime cannot be null");
    }
    if(size != null && size < 0) {
      throw new IllegalArgumentException("size cannot be negative: " + size);
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }

    this.id = id;
    this.parentId = Optional.ofNullable(parentId);
    this.uri = uri;
    this.discoveryTime = discoveryTime;
    this.lastModificationTime = lastModificationTime;
    this.size = Optional.ofNullable(size);
    this.attributes = attributes;
    this.state = state;
    this.duration = Optional.ofNullable(duration);
    this.mediaStructure = Optional.ofNullable(mediaStructure);
    this.snapshots = List.copyOf(snapshots);
    this.match = match;
  }

  public StreamID getId() {
    return id;
  }

  public Optional<StreamID> getParentId() {
    return parentId;
  }

  public URI getUri() {
    return uri;
  }

  /**
   * Returns the time the item was first discovered.
   *
   * @return the time the item was first discovered, never <code>null</code>
   */
  public Instant getDiscoveryTime() {
    return discoveryTime;
  }

  public Instant getLastModificationTime() {
    return lastModificationTime;
  }

  public Optional<Long> getSize() {
    return size;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public State getState() {
    return state;
  }

  public Optional<Duration> getDuration() {
    return duration;
  }

  public Optional<MediaStructure> getMediaStructure() {
    return mediaStructure;
  }

  public List<Snapshot> getSnapshots() {
    return snapshots;
  }

  public Match getMatch() {
    return match;
  }
}

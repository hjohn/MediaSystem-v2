package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.StreamID;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class MediaStream {
  private final StreamID id;
  private final Optional<StreamID> parentId;
  private final StreamAttributes attributes;  // physical attributes
  private final State state;
  private final Optional<Duration> duration;
  private final Optional<MediaStructure> mediaStructure;  // logical attributes (tracks)
  private final List<Snapshot> snapshots;
  private final Optional<Match> match;

  public MediaStream(StreamID id, StreamID parentId, StreamAttributes attributes, State state, Duration duration, MediaStructure mediaStructure, List<Snapshot> snapshots, Match match) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }

    this.id = id;
    this.parentId = Optional.ofNullable(parentId);
    this.attributes = attributes;
    this.state = state;
    this.duration = Optional.ofNullable(duration);
    this.mediaStructure = Optional.ofNullable(mediaStructure);
    this.snapshots = List.copyOf(snapshots);
    this.match = Optional.ofNullable(match);
  }

  public StreamID getId() {
    return id;
  }

  public Optional<StreamID> getParentId() {
    return parentId;
  }

  public StreamAttributes getAttributes() {
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

  public Optional<Match> getMatch() {
    return match;
  }
}

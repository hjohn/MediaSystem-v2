package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.StreamID;

import java.util.Optional;

public class MediaStream {
  private final StreamID id;
  private final Optional<StreamID> parentId;
  private final StreamAttributes attributes;
  private final State state;
  private final Optional<StreamMetaData> metaData;
  private final Optional<Match> match;

  public MediaStream(StreamID id, StreamID parentId, StreamAttributes attributes, State state, StreamMetaData metaData, Match match) {
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
    this.metaData = Optional.ofNullable(metaData);
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

  public Optional<StreamMetaData> getMetaData() {
    return metaData;
  }

  public Optional<Match> getMatch() {
    return match;
  }
}

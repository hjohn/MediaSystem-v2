package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.StreamID;

import java.util.Optional;

public class MediaStream {
  private final StreamID streamId;
  private final Optional<StreamID> parentId;
  private final StreamAttributes attributes;
  private final State state;
  private final Optional<StreamMetaData> metaData;
  private final Optional<Match> match;

  public MediaStream(StreamID streamId, StreamID parentId, StreamAttributes attributes, State state, StreamMetaData metaData, Match match) {
    if(streamId == null) {
      throw new IllegalArgumentException("streamId cannot be null");
    }
    if(state == null) {
      throw new IllegalArgumentException("state cannot be null");
    }
    if(attributes == null) {
      throw new IllegalArgumentException("attributes cannot be null");
    }

    this.streamId = streamId;
    this.parentId = Optional.ofNullable(parentId);
    this.attributes = attributes;
    this.state = state;
    this.metaData = Optional.ofNullable(metaData);
    this.match = Optional.ofNullable(match);
  }

  public StreamID getId() {
    return streamId;
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

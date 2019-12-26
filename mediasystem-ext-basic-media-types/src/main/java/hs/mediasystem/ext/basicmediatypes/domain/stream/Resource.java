package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.scanner.api.StreamID;

import java.util.Optional;

public class Resource {
  private final StreamID streamId;
  private final Optional<StreamID> parentId;
  private final StreamAttributes attributes;
  private final State state;
  private final Optional<StreamMetaData> metaData;
  private final Identification identification;
  private final MediaDescriptor descriptor;

  public Resource(StreamID streamId, StreamID parentId, StreamAttributes attributes, State state, StreamMetaData metaData, Identification identification, MediaDescriptor descriptor) {
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
    this.identification = identification;
    this.descriptor = descriptor;
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

  public Identification getIdentification() {
    return identification;
  }

  public MediaDescriptor getDescriptor() {
    return descriptor;
  }
}

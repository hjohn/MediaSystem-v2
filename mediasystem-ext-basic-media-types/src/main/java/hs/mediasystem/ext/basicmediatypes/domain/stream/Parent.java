package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;

public class Parent {
  private final WorkId id;
  private final MediaType type;
  private final String name;
  private final StreamID streamId;

  public Parent(WorkId id, MediaType type, String name, StreamID streamId) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.streamId = streamId;
  }

  public WorkId getId() {
    return id;
  }

  public MediaType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public StreamID temp_getStreamId() {
    return streamId;
  }
}

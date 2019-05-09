package hs.mediasystem.db;

import hs.mediasystem.scanner.api.StreamID;

import java.util.Map;

public class StreamState {
  private final Map<String, Object> properties;
  private final StreamID streamId;

  public StreamState(StreamID streamId, Map<String, Object> properties) {
    this.streamId = streamId;
    this.properties = properties;
  }

  public StreamID getStreamID() {
    return streamId;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
}

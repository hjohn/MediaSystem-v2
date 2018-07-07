package hs.mediasystem.db;

import java.util.Map;

public class StreamState {
  private final Map<String, Object> properties;
  private final byte[] hash;
  private final Long size;
  private final long lastModificationTime;

  public StreamState(byte[] hash, Long size, long lastModificationTime, Map<String, Object> properties) {
    this.hash = hash;
    this.size = size;
    this.lastModificationTime = lastModificationTime;
    this.properties = properties;
  }

  public byte[] getHash() {
    return hash;
  }

  public Long getSize() {
    return size;
  }

  public long getLastModificationTime() {
    return lastModificationTime;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
}

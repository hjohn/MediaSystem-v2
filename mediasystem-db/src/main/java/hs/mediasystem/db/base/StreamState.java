package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.ContentID;

import java.util.Map;

public class StreamState {
  private final Map<String, Object> properties;
  private final ContentID contentId;

  public StreamState(ContentID contentId, Map<String, Object> properties) {
    this.contentId = contentId;
    this.properties = properties;
  }

  public ContentID getContentID() {
    return contentId;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }
}

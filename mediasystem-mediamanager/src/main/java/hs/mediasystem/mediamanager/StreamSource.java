package hs.mediasystem.mediamanager;

public class StreamSource {
  private final StreamTags tags;
  private final String dataSourceName;

  public StreamSource(StreamTags tags, String dataSourceName) {
    this.tags = tags;
    this.dataSourceName = dataSourceName;
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

  public StreamTags getTags() {
    return tags;
  }
}

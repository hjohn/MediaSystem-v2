package hs.mediasystem.mediamanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamSource {
  private final StreamTags tags;
  private final List<String> dataSourceNames;

  public StreamSource(StreamTags tags, List<String> dataSourceNames) {
    this.tags = tags;
    this.dataSourceNames = Collections.unmodifiableList(new ArrayList<>(dataSourceNames));
  }

  public List<String> getDataSourceNames() {
    return dataSourceNames;
  }

  public StreamTags getTags() {
    return tags;
  }
}

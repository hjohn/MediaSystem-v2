package hs.mediasystem.mediamanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record StreamSource(StreamTags tags, List<String> dataSourceNames) {
  public StreamSource {
    dataSourceNames = Collections.unmodifiableList(new ArrayList<>(dataSourceNames));
  }
}

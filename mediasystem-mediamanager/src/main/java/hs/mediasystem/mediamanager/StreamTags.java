package hs.mediasystem.mediamanager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StreamTags {
  private final Set<String> tags;

  public StreamTags(Set<String> tags) {
    this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
  }

  public boolean contains(String tag) {
    return tags.contains(tag);
  }

  @Override
  public String toString() {
    return "StreamTags[" + tags + "]";
  }
}

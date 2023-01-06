package hs.mediasystem.ext.basicmediatypes.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record StreamTags(Set<String> tags) {

  public StreamTags {
    tags = Collections.unmodifiableSet(new HashSet<>(tags));
  }

  public boolean contains(String tag) {
    return tags.contains(tag);
  }
}

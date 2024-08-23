package hs.mediasystem.db.core.domain;

import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.Match;

import java.net.URI;
import java.util.List;

public record Resource(Streamable streamable, Match match, List<? extends Release> releases) {

  public Resource {
    if(streamable == null) {
      throw new IllegalArgumentException("streamable cannot be null");
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }
    if(releases == null || releases.isEmpty()) {
      throw new IllegalArgumentException("releases cannot be null or empty: " + releases);
    }
  }

  public URI location() {
    return streamable.location();
  }

  public ContentID contentId() {
    return streamable.contentId();
  }
}
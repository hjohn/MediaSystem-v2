package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Match;

import java.util.List;

public record LinkedResource(Resource resource, Match match, List<Work> works) {
  public LinkedResource {
    if(resource == null) {
      throw new IllegalArgumentException("resource cannot be null");
    }
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }
    if(works == null || works.isEmpty()) {
      throw new IllegalArgumentException("works cannot be null or empty: " + works);
    }
  }

  public StreamID id() {
    return resource.id();
  }
}

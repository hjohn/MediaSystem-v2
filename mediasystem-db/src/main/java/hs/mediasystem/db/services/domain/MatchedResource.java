package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.work.Match;

public record MatchedResource(Match match, Resource resource) {
  public MatchedResource {
    if(match == null) {
      throw new IllegalArgumentException("match cannot be null");
    }
    if(resource == null) {
      throw new IllegalArgumentException("resource cannot be null");
    }
  }
}

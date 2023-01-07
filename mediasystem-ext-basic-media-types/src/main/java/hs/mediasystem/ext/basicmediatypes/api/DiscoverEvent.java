package hs.mediasystem.ext.basicmediatypes.api;

import hs.mediasystem.util.domain.URIs;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public record DiscoverEvent(URI base, Optional<String> identificationService, StreamTags tags, List<Discovery> discoveries) {
  public DiscoverEvent {
    if(base == null) {
      throw new IllegalArgumentException("base cannot be null");
    }
    if(identificationService == null) {
      throw new IllegalArgumentException("identificationService cannot be null");
    }
    if(discoveries == null) {
      throw new IllegalArgumentException("discoveries cannot be null");
    }

    if(discoveries.stream().map(Discovery::parentLocation).distinct().limit(2).count() > 1) {
      throw new IllegalArgumentException("discoveries cannot contain items with different parents: " + discoveries);
    }

    base = URIs.normalizeAsFolder(base);
  }
}

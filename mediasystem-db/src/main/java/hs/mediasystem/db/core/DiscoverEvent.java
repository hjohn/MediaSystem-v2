package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.util.domain.URIs;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public record DiscoverEvent(URI base, Optional<IdentificationProvider> identificationProvider, StreamTags tags, Optional<URI> parentLocation, List<Discovery> discoveries) {
  public DiscoverEvent {
    if(base == null) {
      throw new IllegalArgumentException("base cannot be null");
    }
    if(identificationProvider == null) {
      throw new IllegalArgumentException("identificationProvider cannot be null");
    }
    if(tags == null) {
      throw new IllegalArgumentException("tags cannot be null");
    }
    if(parentLocation == null) {
      throw new IllegalArgumentException("parentLocation cannot be null");
    }
    if(discoveries == null) {
      throw new IllegalArgumentException("discoveries cannot be null");
    }

    base = URIs.normalizeAsFolder(base);
  }
}

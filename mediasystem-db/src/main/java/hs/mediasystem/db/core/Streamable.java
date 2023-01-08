package hs.mediasystem.db.core;

import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.domain.stream.ContentID;

import java.net.URI;
import java.util.Optional;

public record Streamable(Discovery discovery, Optional<String> identificationService, StreamTags tags) {

  public Streamable {
    if(discovery == null) {
      throw new IllegalArgumentException("discovery cannot be null");
    }
    if(identificationService == null) {
      throw new IllegalArgumentException("identificationService cannot be null");
    }
    if(tags == null) {
      throw new IllegalArgumentException("tags cannot be null");
    }
  }

  public URI location() {
    return discovery.location();
  }

  public ContentID contentId() {
    return discovery.contentPrint().getId();
  }
}

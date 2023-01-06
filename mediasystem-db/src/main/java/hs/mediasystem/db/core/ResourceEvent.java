package hs.mediasystem.db.core;

import hs.mediasystem.db.services.domain.Resource;

import java.net.URI;

public sealed interface ResourceEvent {

  URI location();

  public record Updated(Resource resource) implements ResourceEvent {
    @Override
    public URI location() {
      return resource.location();
    }
  }

  public record Removed(URI location) implements ResourceEvent {}
}

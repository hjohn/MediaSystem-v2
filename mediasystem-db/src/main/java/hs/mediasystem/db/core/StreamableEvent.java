package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;

import java.net.URI;
import java.util.Optional;

public sealed interface StreamableEvent {

  URI location();

  public record Updated(Streamable streamable, Optional<IdentificationProvider> identificationProvider, Discovery discovery) implements StreamableEvent {
    @Override
    public URI location() {
      return streamable.location();
    }
  }

  public record Removed(URI location) implements StreamableEvent {}
}

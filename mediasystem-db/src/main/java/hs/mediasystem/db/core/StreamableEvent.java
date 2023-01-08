package hs.mediasystem.db.core;

import java.net.URI;

public sealed interface StreamableEvent {

  URI location();

  public record Updated(Streamable streamable) implements StreamableEvent {
    @Override
    public URI location() {
      return streamable.location();
    }
  }

  public record Removed(URI location) implements StreamableEvent {}
}

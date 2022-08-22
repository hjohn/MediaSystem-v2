package hs.mediasystem.db.base;

import hs.mediasystem.domain.stream.StreamID;

public interface StreamableEvent {
  public record Updated(CachedStream cachedStream) implements StreamableEvent {}
  public record Removed(StreamID id) implements StreamableEvent {}
}

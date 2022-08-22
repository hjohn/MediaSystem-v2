package hs.mediasystem.db.services;

import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.domain.stream.StreamID;

public interface LinkedResourceEvent {
  public record Updated(LinkedResource resource) implements LinkedResourceEvent {}
  public record Removed(StreamID id) implements LinkedResourceEvent {}
}

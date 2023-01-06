package hs.mediasystem.db.services;

import hs.mediasystem.db.services.domain.LinkedResource;

import java.net.URI;

public interface LinkedResourceEvent {
  public record Updated(LinkedResource resource) implements LinkedResourceEvent {}
  public record Removed(URI location) implements LinkedResourceEvent {}
}

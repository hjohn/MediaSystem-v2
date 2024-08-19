package hs.mediasystem.db.services;

import hs.mediasystem.db.services.domain.Resource;

import java.net.URI;

interface ResourceEvent {
  record Updated(Resource resource) implements ResourceEvent {}
  record Removed(URI location) implements ResourceEvent {}
}

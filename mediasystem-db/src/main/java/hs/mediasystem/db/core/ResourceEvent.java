package hs.mediasystem.db.core;

import hs.mediasystem.db.core.domain.Resource;

import java.net.URI;

interface ResourceEvent {
  record Updated(Resource resource) implements ResourceEvent {}
  record Removed(URI location) implements ResourceEvent {}
}

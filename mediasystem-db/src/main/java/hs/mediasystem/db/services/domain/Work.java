package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;

import java.util.Optional;

public record Work(WorkDescriptor descriptor, Optional<Parent> parent) {
  public WorkId id() {
    return descriptor.getId();
  }
}
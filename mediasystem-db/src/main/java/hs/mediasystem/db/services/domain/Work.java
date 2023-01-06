package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;

public record Work(WorkDescriptor descriptor) {
  public WorkId id() {
    return descriptor.getId();
  }
}
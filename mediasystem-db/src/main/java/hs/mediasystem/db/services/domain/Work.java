package hs.mediasystem.db.services.domain;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.domain.work.WorkId;

public record Work(WorkDescriptor descriptor) {
  public WorkId id() {
    return descriptor.getId();
  }
}
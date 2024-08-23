package hs.mediasystem.db.core.domain;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.domain.work.WorkId;

import java.util.List;

/**
 * Represents a {@link WorkDescriptor} including any resources that match it.
 *
 * @param workDescriptor a {@link WorkDescriptor}, cannot be {@code null}
 * @param resources a list of {@link Resource}s, cannot be {@code null}, contain {@code null}s or be empty
 */
public record LinkedWork(WorkDescriptor workDescriptor, List<Resource> resources) {

  public LinkedWork {
    if(workDescriptor == null) {
      throw new IllegalArgumentException("workDescriptor cannot be null");
    }
    if(resources == null || resources.isEmpty()) {
      throw new IllegalArgumentException("resources cannot be null or empty: " + resources);
    }
  }

  public WorkId id() {
    return workDescriptor.id();
  }
}


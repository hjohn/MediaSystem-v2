package hs.mediasystem.db.services.domain;

import hs.mediasystem.domain.work.WorkId;

import java.util.List;

/**
 * Represents a {@link Work} including any resources that match this work.
 *
 * @param work a {@link Work}, cannot be {@code null}
 * @param matchedResources a list of {@link MatchedResource}, cannot be {@code null}, contain {@code null}s or be empty
 */
public record LinkedWork(Work work, List<MatchedResource> matchedResources) {

  public LinkedWork {
    if(work == null) {
      throw new IllegalArgumentException("work cannot be null");
    }
    if(matchedResources == null || matchedResources.isEmpty()) {
      throw new IllegalArgumentException("matchedResources cannot be null or empty: " + matchedResources);
    }
  }

  public WorkId id() {
    return work.id();
  }
}


package hs.mediasystem.api.datasource.domain;

import hs.mediasystem.domain.work.WorkId;

public class CollectionDetails {
  private final WorkId id;
  private final Details details;

  public CollectionDetails(WorkId id, Details details) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }

    this.id = id;
    this.details = details;
  }

  public WorkId getId() {
    return id;
  }

  public Details getDetails() {
    return details;
  }
}

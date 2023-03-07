package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;

import java.util.List;

public class WorksGroup {
  private final WorkId id;
  private final Details details;
  private final List<Work> children;
  private final boolean allWatched;

  public WorksGroup(WorkId id, Details details, List<Work> children, boolean allWatched) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(details == null) {
      throw new IllegalArgumentException("details cannot be null");
    }
    if(children == null) {
      throw new IllegalArgumentException("children cannot be null");
    }

    this.id = id;
    this.details = details;
    this.children = children;
    this.allWatched = allWatched;
  }

  public WorkId getId() {
    return id;
  }

  public Details getDetails() {
    return details;
  }

  public List<Work> getChildren() {
    return children;
  }

  public boolean allWatched() {
    return allWatched;
  }
}

package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.work.WorkId;

import java.util.List;

public class WorkIdCollection extends AbstractCollection<WorkId> {

  public WorkIdCollection(CollectionDetails collectionDetails, List<WorkId> items) {
    super(collectionDetails, items);
  }

}

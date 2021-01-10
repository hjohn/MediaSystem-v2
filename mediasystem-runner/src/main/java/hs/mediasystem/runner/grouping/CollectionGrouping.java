package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CollectionGrouping implements Grouping<Work, Object> {
  @Inject private WorkClient workClient;

  @Override
  public List<Object> group(List<? extends Work> items) {
    Map<WorkId, List<Work>> childWorks = new HashMap<>();
    List<Object> topLevelItems = new ArrayList<>();

    for(Work work : items) {
      work.getParent().filter(p -> p.getType().equals(MediaType.COLLECTION)).ifPresent(p -> {
        if(!childWorks.containsKey(p.getId())) {
          workClient.find(p.getId()).ifPresent(r -> {
            childWorks.put(p.getId(), workClient.findChildren(p.getId()));

            topLevelItems.add(createWorkGroup(r, childWorks.get(p.getId())));
          });
        }
      });

      if(work.getParent().filter(p -> childWorks.containsKey(p.getId())).isEmpty()) {
        topLevelItems.add(work);
      }
    }

    return topLevelItems;
  }

  private static WorksGroup createWorkGroup(Work work, List<Work> list) {
    return new WorksGroup(work.getId(), work.getDetails(), list);
  }
}

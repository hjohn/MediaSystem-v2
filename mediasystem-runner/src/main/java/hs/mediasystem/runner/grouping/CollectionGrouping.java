package hs.mediasystem.runner.grouping;

import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;
import hs.mediasystem.scanner.api.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CollectionGrouping implements Grouping<Work> {
  private static final MediaType COLLECTION = MediaType.of("COLLECTION");

  @Inject private WorkService workService;

  @Override
  public List<Object> group(List<Work> items) {
    Map<WorkId, List<Work>> childWorks = new HashMap<>();
    List<Object> topLevelItems = new ArrayList<>();

    for(Work work : items) {
      work.getParent().filter(p -> p.getType().equals(COLLECTION)).ifPresent(p -> {
        if(!childWorks.containsKey(p.getId())) {
          workService.find(p.getId()).ifPresent(r -> {
            childWorks.put(p.getId(), workService.findChildren(p.getId()));

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

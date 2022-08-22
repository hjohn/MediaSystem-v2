package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    LocalDate now = LocalDate.now().plusDays(1);
    Map<WorkId, List<Work>> childWorks = new HashMap<>();
    List<Object> topLevelItems = new ArrayList<>();

    for(Work work : items) {
      work.getParent().filter(p -> p.type().equals(MediaType.COLLECTION)).ifPresent(p -> {
        if(!childWorks.containsKey(p.id())) {
          workClient.find(p.id()).ifPresent(r -> {
            List<Work> children = workClient.findChildren(p.id()).stream()
              .filter(c -> now.isAfter(c.getDetails().getReleaseDate().orElse(LocalDate.MAX)))
              .toList();

            if(children.size() > 1) {  // don't bother creating a group when it contains 1 item
              childWorks.put(p.id(), children);

              topLevelItems.add(createWorkGroup(r, childWorks.get(p.id()), now));
            }
          });
        }
      });

      if(work.getParent().filter(p -> childWorks.containsKey(p.id())).isEmpty()) {
        topLevelItems.add(work);
      }
    }

    return topLevelItems;
  }

  private static WorksGroup createWorkGroup(Work work, List<Work> list, LocalDate now) {
    Work latest = list.stream()
      .sorted(Comparator.comparing(Work::getDetails, Details.RELEASE_DATE_REVERSED))
      .filter(c -> now.isAfter(c.getDetails().getReleaseDate().orElse(LocalDate.MAX)))
      .findFirst()
      .orElse(work);

    boolean watched = list.stream().allMatch(Work::isWatched);

    Details latestDetails = latest.getDetails();
    Details groupDetails = work.getDetails();

    Details combinedDetails = new Details(
      groupDetails.getTitle(),
      groupDetails.getSubtitle().orElse(null),
      groupDetails.getDescription().orElse(null),
      latestDetails.getReleaseDate().orElse(null),
      groupDetails.getCover().or(latestDetails::getCover).orElse(null),
      groupDetails.getAutoCover().or(latestDetails::getAutoCover).orElse(null),
      groupDetails.getSampleImage().or(latestDetails::getSampleImage).orElse(null),
      groupDetails.getBackdrop().or(latestDetails::getBackdrop).orElse(null),
      groupDetails.getTagline().orElse(null),
      groupDetails.getSerie().orElse(null),
      groupDetails.getSequence().orElse(null),
      groupDetails.getReception().orElse(null),
      groupDetails.getPopularity().orElse(null),
      groupDetails.getClassification()
    );

    return new WorksGroup(work.getId(), combinedDetails, list, watched);
  }
}

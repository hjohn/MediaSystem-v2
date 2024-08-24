package hs.mediasystem.runner.grouping;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.domain.Classification;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ImageHandleFactory;
import hs.mediasystem.util.image.ImageURI;
import hs.mediasystem.util.natural.NaturalLanguage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AlphabeticalGrouping implements Grouping<Work, Object> {
  private static final DataSource DATA_SOURCE = DataSource.instance("ALPHA-GROUPED");
  private static final Comparator<Work> WEIGHTED_RATING_COMPARATOR = Comparator.comparing(AlphabeticalGrouping::score).reversed();

  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public List<Object> group(List<? extends Work> items) {
    Map<String, List<Work>> numericMap = new HashMap<>();
    Map<String, List<Work>> alphaMap = new HashMap<>();
    int totalNumericEntries = 0;
    int totalAlphaEntries = 0;

    /*
     * In order that the created groups start and end at their extreme values (even
     * if those are not present), ensure that these are present, even if empty:
     */

    numericMap.put("0", new ArrayList<>());
    numericMap.put("9", new ArrayList<>());
    alphaMap.put("A", new ArrayList<>());
    alphaMap.put("Z", new ArrayList<>());

    /*
     * Create groups:
     */

    for(Work item : items) {
      char c = Character.toUpperCase(NaturalLanguage.stripArticle(item.getDetails().getTitle()).charAt(0));

      if(c >= 'A' && c <= 'Z') {
        alphaMap.computeIfAbsent(Character.toString(c), k -> new ArrayList<>()).add(item);
        totalAlphaEntries++;
      }
      else if(c >= '0' && c <= '9') {
        numericMap.computeIfAbsent(Character.toString(c), k -> new ArrayList<>()).add(item);
        totalNumericEntries++;
      }
      else {
        numericMap.computeIfAbsent("0", k -> new ArrayList<>()).add(item);
        totalNumericEntries++;
      }
    }

    int totalGroupCount = 12;
    int numericGroupCount = totalNumericEntries == 0 ? 0 : Math.max(1, (int)((double)totalNumericEntries / (totalNumericEntries + totalAlphaEntries) * totalGroupCount));
    int alphaGroupCount = totalGroupCount - numericGroupCount;

    Map<String, List<Work>> map = createOptimalGroups(numericMap, numericGroupCount);

    map.putAll(createOptimalGroups(alphaMap, alphaGroupCount));

    /*
     * Create the top level "works" with cover images derived from the contained
     * items:
     */

    List<Object> topLevelItems = new ArrayList<>();

    for(Map.Entry<String, List<Work>> entry : map.entrySet()) {

      /*
       * This picks 4 covers that best represent this section.
       */

      AtomicReference<ImageHandle> backgroundImageHandleRef = new AtomicReference<>();
      String uris = entry.getValue().stream()
        .sorted(WEIGHTED_RATING_COMPARATOR)
        .peek(mi -> {  // This is dirty
          if(backgroundImageHandleRef.get() == null) {
            mi.getDetails().getBackdrop().ifPresent(backgroundImageHandleRef::set);
          }
        })
        .map(Work::getDetails)
        .map(Details::getCover)
        .flatMap(Optional::stream)
        .filter(Objects::nonNull)
        .map(ImageHandle::getKey)
        .limit(4)
        .collect(Collectors.joining("|"));

      Details details = new Details(
        entry.getKey(),
        null,
        null,
        null,
        uris.isEmpty() ? null : imageHandleFactory.fromURI(new ImageURI("multi::" + uris, null)),  // as cover
        null,
        null,
        backgroundImageHandleRef.get(),
        null,
        null,
        null,
        null,
        null,
        Classification.DEFAULT
      );

      List<Work> children = entry.getValue();

      WorksGroup parent = new WorksGroup(
        new WorkId(DATA_SOURCE, MediaType.FOLDER, entry.getKey()),
        details,
        children,
        false
      );

      topLevelItems.add(parent);
    }

    return topLevelItems;
  }

  private static double score(Work work) {
    Reception reception = work.getDetails().getReception().orElse(null);
    LocalDate date = work.getDetails().getReleaseDate().orElse(null);

    if(reception != null && date != null && !Boolean.TRUE.equals(work.getDetails().getClassification().pornographic())) {
      return reception.rating() + date.getYear() * 0.05;
    }

    return Double.NEGATIVE_INFINITY;
  }

  // Note: this function takes O(input.size() ^ 2 * desiredGroupCount) time and O(input.size() * desiredGroupCount) space.
  // Using it for creating alpha groups (A-Z) with up to 26 groups is safe. Careful when using it for other use cases.
  private static <T> Map<String, List<T>> createOptimalGroups(Map<String, List<T>> input, int desiredGroupCount) {
    if(input.size() <= desiredGroupCount) {
      return input;
    }

    List<String> keys = new ArrayList<>(input.keySet());
    int keyCount = keys.size();
    int groupCount = Math.min(desiredGroupCount, keyCount);  // Limit number of groups if there is a lack of keys

    Collections.sort(keys); // Sort keys to create consecutive groups

    /*
     * Calculate cumulative sums of the sizes of lists:
     */

    int[] cumulativeSum = new int[keyCount + 1];  // one larger than keys [0, size(0), size(0) + size(1), etc]

    for(int i = 0; i < keyCount; i++) {
      cumulativeSum[i + 1] = cumulativeSum[i] + input.get(keys.get(i)).size();
    }

    /*
     * Initialize structures (space needed is a few kB with 26 keys and 12 groups):
     */

    int[][] costs = new int[keyCount + 1][groupCount + 1];
    int[][] partition = new int[keyCount + 1][groupCount + 1];

    for(int i = 0; i <= keyCount; i++) {
      Arrays.fill(costs[i], Integer.MAX_VALUE / 2);
    }

    costs[0][0] = 0;

    /*
     * Try to minimize the difference between the size of each group and the target size
     * (total size / group count) by trying all possible partitions. For 26 keys and 12
     * groups the complexity is O(26^2 * 12):
     */

    for(int i = 1; i <= keyCount; i++) {
      for(int j = 1; j <= groupCount; j++) {
        for(int k = 0; k < i; k++) {
          int currentSum = cumulativeSum[i] - cumulativeSum[k];
          int cost = costs[k][j - 1] + Math.abs(currentSum - (cumulativeSum[keyCount] / groupCount));

          if(cost < costs[i][j]) {
            costs[i][j] = cost;
            partition[i][j] = k;
          }
        }
      }
    }

    /*
     * Create optimal groups:
     */

    Map<String, List<T>> result = new HashMap<>();
    int index = keyCount;

    for(int j = groupCount; j > 0; j--) {
      List<T> group = new ArrayList<>();
      int start = partition[index][j];

      for(int i = start; i < index; i++) {
        group.addAll(input.get(keys.get(i)));
      }

      if(start == index - 1) {
        result.put(keys.get(start), group);
      }
      else {
        result.put(keys.get(start) + "-" + keys.get(index - 1), group);
      }

      index = start;
    }

    return result;
  }
}

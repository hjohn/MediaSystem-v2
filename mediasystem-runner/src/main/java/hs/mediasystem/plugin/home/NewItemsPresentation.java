package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

public class NewItemsPresentation {
  public final ObjectProperty<Item> selectedItem = new SimpleObjectProperty<>();

  private final List<Item> newItems;

  @Singleton
  public static class Factory {
    @Inject private RecommendationClient recommendationClient;

    public NewItemsPresentation create() {
      List<Item> newItems = groupRecommendations(recommendationClient.findNew(mediaType -> !mediaType.isSerie() && mediaType != MediaType.FOLDER && mediaType != MediaType.FILE));

      return new NewItemsPresentation(newItems);
    }
  }

  public NewItemsPresentation(List<Item> newItems) {
    this.newItems = newItems;
  }

  public List<Item> getNewItems() {
    return newItems;
  }

  private static List<Item> groupRecommendations(List<Recommendation> recommendations) {
    Map<String, ConsolidatedNewItem> groupedMap = new HashMap<>();

    /*
     * Recommendations are in order of newest to oldest, the order of the sample time field
     */

    for(Recommendation recommendation : recommendations) {
      Work work = recommendation.getWork();
      Sequence sequence = work.getDetails().getSequence().orElse(null);
      Parent parent = work.getParent().orElse(null);
      boolean hasParent = recommendation.getWork().getType().isComponent();

      if(!hasParent || parent == null || sequence == null || sequence.getSeasonNumber().orElse(0) <= 0) {
        groupedMap.put(work.getId().toString(), new ConsolidatedNewItem(recommendation, 0));
      }
      else {
        ConsolidatedNewItem item = groupedMap.computeIfAbsent(parent.getId().toString(), k -> new ConsolidatedNewItem(recommendation, -1));

        item.similarCount++;
        item.seasonCounts.merge(sequence.getSeasonNumber().orElse(0), 1, (a, b) -> a + b);

        if(Sequence.COMPARATOR.compare(sequence, item.recommendation.getWork().getDetails().getSequence().orElseThrow()) < 0) {
          item.recommendation = recommendation;
        }
      }
    }

    return groupedMap.values().stream()
      .sorted(Comparator.comparing((ConsolidatedNewItem cni) -> cni.recommendation.getSampleTime()).reversed())
      .map(NewItemsPresentation::toItem)
      .collect(Collectors.toList());
  }

  private static Item toItem(ConsolidatedNewItem item) {
    Recommendation recommendation = item.recommendation;

    if(item.similarCount == 0) {
      return toItem(recommendation);
    }

    Work work = recommendation.getWork();

    return new Item(
      recommendation,
      null,
      work.getParent().orElseThrow().getName(),
      createSubtitle(item)
    );
  }

  private static String createSubtitle(ConsolidatedNewItem item) {
    if(item.seasonCounts.size() == 1) {
      return "Season " + item.seasonCounts.firstKey() + ": " + (item.similarCount + 1) + " new episode" + (item.similarCount > 0 ? "s" : "");
    }

    return (item.similarCount + 1) + " new episodes in " + item.seasonCounts.size() + " seasons";
  }

  private static Item toItem(Recommendation recommendation) {
    boolean hasParent = recommendation.getWork().getType().isComponent();

    return new Item(
      recommendation,
      hasParent ? recommendation.getWork().getParent().map(p -> p.getName()).orElse(null) : null,
      recommendation.getWork().getDetails().getTitle(),
      !hasParent ? recommendation.getWork().getDetails().getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse(null) : null
    );
  }

  public static class Item {
    public final Recommendation recommendation;
    public final String parentTitle;
    public final String title;
    public final String subtitle;

    public Item(Recommendation recommendation, String parentTitle, String title, String subtitle) {
      this.recommendation = recommendation;
      this.parentTitle = parentTitle;
      this.title = title;
      this.subtitle = subtitle;
    }
  }

  private static class ConsolidatedNewItem {
    public final SortedMap<Integer, Integer> seasonCounts = new TreeMap<>();

    public Recommendation recommendation;
    public int similarCount;

    public ConsolidatedNewItem(Recommendation recommendation, int similarCount) {
      this.recommendation = recommendation;
      this.similarCount = similarCount;
    }
  }
}

package hs.mediasystem.plugin.home;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ui.api.RecommendationClient;
import hs.mediasystem.ui.api.domain.Parent;
import hs.mediasystem.ui.api.domain.Recommendation;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.ArrayList;
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
    this.selectedItem.set(newItems.isEmpty() ? null : newItems.get(0));
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
      Work work = recommendation.work();
      Sequence sequence = work.getDetails().getSequence().orElse(null);
      Parent parent = work.getParent().orElse(null);
      boolean hasParent = recommendation.work().getType().isComponent();

      if(!hasParent || parent == null || sequence == null) {
        groupedMap.put(work.getId().toString(), new ConsolidatedNewItem(recommendation));
      }
      else {
        ConsolidatedNewItem item = groupedMap.computeIfAbsent(parent.id().toString(), k -> new ConsolidatedNewItem(recommendation));

        if(sequence.type() == Type.EPISODE) {
          item.episodes++;
          item.seasonCounts.merge(sequence.seasonNumber().orElse(0), 1, (a, b) -> a + b);
        }
        else if(sequence.type() == Type.SPECIAL) {
          item.specials++;
        }
        else {
          item.extras++;
        }

        if(Sequence.COMPARATOR.compare(sequence, item.recommendation.work().getDetails().getSequence().orElseThrow()) < 0) {
          item.recommendation = recommendation;
        }
      }
    }

    return groupedMap.values().stream()
      .sorted(Comparator.comparing((ConsolidatedNewItem cni) -> cni.recommendation.sampleTime()).reversed())
      .map(NewItemsPresentation::toItem)
      .collect(Collectors.toList());
  }

  private static Item toItem(ConsolidatedNewItem item) {
    Recommendation recommendation = item.recommendation;

    if(item.episodes <= 1 && item.specials == 0 && item.extras == 0) {
      return toItem(recommendation);
    }

    Work work = recommendation.work();

    return new Item(
      recommendation,
      null,
      work.getParent().orElseThrow().title(),
      createSubtitle(item)
    );
  }

  private static String createSubtitle(ConsolidatedNewItem item) {
    StringBuilder builder = new StringBuilder();

    if(item.seasonCounts.size() == 1) {
      builder.append("Season " + item.seasonCounts.firstKey() + ": ");
    }
    else if(item.seasonCounts.size() > 1) {
      builder.append(item.seasonCounts.size() + " new seasons");
    }

    List<String> parts = new ArrayList<>();

    if(item.episodes > 0) {
      parts.add(item.episodes == 1 ? "a new episode" : item.episodes + " new episodes");
    }

    if(item.specials > 0) {
      parts.add(item.specials == 1 ? "a new special" : item.specials + " new specials");
    }

    if(item.extras > 0) {
      parts.add(item.extras == 1 ? "a new extra" : item.extras + " new extras");
    }

    for(int i = 0; i < parts.size(); i++) {
      String part = parts.get(i);

      if(builder.isEmpty()) {
        builder.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
      }
      else {
        builder.append(builder.charAt(builder.length() - 1) == ' ' ? "" : i == parts.size() - 1 ? " and " : ", ");
        builder.append(part);
      }
    }

    return builder.toString();
  }

  private static Item toItem(Recommendation recommendation) {
    boolean hasParent = recommendation.work().getType().isComponent();

    return new Item(
      recommendation,
      hasParent ? recommendation.work().getParent().map(Parent::title).orElse(null) : null,
      recommendation.work().getDetails().getTitle(),
      !hasParent ? recommendation.work().getDetails().getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse(null) : null
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
    public int episodes;
    public int extras;
    public int specials;

    public ConsolidatedNewItem(Recommendation recommendation) {
      this.recommendation = recommendation;
    }
  }
}

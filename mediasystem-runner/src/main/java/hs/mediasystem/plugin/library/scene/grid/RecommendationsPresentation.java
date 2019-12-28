package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;
import hs.mediasystem.plugin.library.scene.WorkBinder;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

public class RecommendationsPresentation extends GridViewPresentation<Work> {

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("best", (a, b) -> 0),
    new SortOrder<>("alpha", WorkBinder.BY_NAME),
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed())
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("available", r -> r.getPrimaryStream().isPresent()),
    new Filter<>("unwatched", r -> r.getPrimaryStream().isPresent() && !r.getState().isWatched())
  );

  @Singleton
  public static class Factory {
    @Inject private WorkService workService;

    public RecommendationsPresentation create(Production production) {
      return new RecommendationsPresentation(
        production,
        FXCollections.observableList(workService.findRecommendations(new WorkId(production.getIdentifier())))
      );
    }
  }

  protected RecommendationsPresentation(Production production, ObservableList<Work> recommendations) {
    super(recommendations, new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), production);
  }
}

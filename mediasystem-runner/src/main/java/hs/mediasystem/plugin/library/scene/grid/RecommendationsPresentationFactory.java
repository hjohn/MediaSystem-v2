package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationsPresentationFactory extends GridViewPresentationFactory {

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("best", (a, b) -> 0),
    new SortOrder<>("alpha", WorkBinder.BY_NAME),
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed())
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("available", r -> r.getPrimaryStream().isPresent()),
    new Filter<>("unwatched", r -> r.getPrimaryStream().isPresent() && !r.getState().isConsumed().getValue())
  );

  @Inject private WorkClient workClient;

  public RecommendationsPresentation create(Work work) {
    return new RecommendationsPresentation(work);
  }

  public class RecommendationsPresentation extends GridViewPresentation<Work> {
    public RecommendationsPresentation(Work work) {
      super("Recommendations", FXCollections.observableList(workClient.findRecommendations(work.getId())), new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), work);
    }
  }
}

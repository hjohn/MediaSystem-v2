package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;

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
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("available", r -> r.getPrimaryStream().isPresent()),
    new Filter<>("unwatched", r -> r.getPrimaryStream().isPresent() && !r.getState().isConsumed().getValue())
  );

  @Singleton
  public static class Factory {
    @Inject private WorkClient workClient;
    @Inject private SettingsClient settingsClient;
    @Inject private BinderProvider binderProvider;

    public RecommendationsPresentation create(Work work) {
      return new RecommendationsPresentation(
        settingsClient,
        binderProvider,
        work,
        FXCollections.observableList(workClient.findRecommendations(work.getId()))
      );
    }
  }

  protected RecommendationsPresentation(SettingsClient settingsClient, BinderProvider binderProvider, Work work, ObservableList<Work> recommendations) {
    super(settingsClient.of(SYSTEM_PREFIX + "Recommendations"), binderProvider, recommendations, new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), work);
  }
}

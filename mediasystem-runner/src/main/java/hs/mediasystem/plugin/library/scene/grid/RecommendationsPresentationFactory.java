package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationsPresentationFactory extends GridViewPresentationFactory {

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("best", (a, b) -> 0),
    new SortOrder<>("alpha", Work.BY_NAME),
    new SortOrder<>("release-date", Work.BY_RELEASE_DATE.reversed())
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("available", r -> r.getPrimaryStream().isPresent()),
    new Filter<>("unwatched", r -> r.getPrimaryStream().isPresent() && !r.getState().consumed())
  );

  @Inject private WorkClient workClient;

  public RecommendationsPresentation create(WorkId id) {
    return new RecommendationsPresentation(id);
  }

  public class RecommendationsPresentation extends GridViewPresentation<Work, Work> {
    private final WorkId id;

    public RecommendationsPresentation(WorkId id) {
      super("Recommendations", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), Work::getId);

      this.id = id;

      createUpdateTask().run();
    }

    @Override
    public Runnable createUpdateTask() {
      Work work = workClient.find(id).orElseThrow(() -> new WorkNotFoundException(id));
      List<Work> recommendations = workClient.findRecommendations(id);

      return () -> {
        this.rootContextItem.set(work);
        this.inputItems.set(recommendations);
      };
    }
  }
}

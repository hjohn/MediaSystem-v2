package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductionCollectionFactory {
  @Inject private GenericCollectionPresentationFactory factory;
  @Inject private WorkClient workClient;

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", WorkBinder.BY_NAME)
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

  public GenericCollectionPresentation<Work> create(WorkId id) {
    Work collection = workClient.find(id).orElseThrow();
    List<Work> children = workClient.findChildren(id);

    return factory.create(children, "Collections", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), collection);
  }
}

package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.library.scene.grid.common.WorkNotFoundException;
import hs.mediasystem.plugin.library.scene.grid.common.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.common.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.common.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.plugin.library.scene.grid.generic.GenericCollectionPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.generic.GenericCollectionPresentationFactory.GenericCollectionPresentation;
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
    new SortOrder<>("release-date", Work.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", Work.BY_NAME)
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

  public GenericCollectionPresentation<Work, Work> create(WorkId id) {
    return factory.create(
      () -> workClient.findChildren(id),
      "Collections",
      new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS),
      () -> workClient.find(id).orElseThrow(() -> new WorkNotFoundException(id)),
      Work::getId
    );
  }
}

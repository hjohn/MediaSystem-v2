package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.ViewOptions;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProductionCollectionFactory {
  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private WorkService workService;

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", WorkBinder.BY_NAME)
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

  public GenericCollectionPresentation<Work> create(WorkId id) {
    Work collection = workService.find(id).orElseThrow();
    List<Work> children = workService.findChildren(id);

    return factory.create(children, "Collections", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), collection);
  }
}

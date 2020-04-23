package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesCollectionType implements CollectionType {

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", WorkBinder.BY_NAME),
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("watched-date", WorkBinder.BY_LAST_WATCHED_DATE.reversed())
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent()),
    new Filter<>("watched-recently", r -> r.getState().getLastConsumptionTime().filter(d -> d.isAfter(Instant.now().minus(365 * 2, ChronoUnit.DAYS))).isPresent()
    )
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("unwatched", r -> !r.getState().isConsumed().getValue())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private WorksClient worksClient;

  @Override
  public String getId() {
    return MediaType.SERIE.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      worksClient.findAllByType(MediaType.SERIE, tag),
      "Series" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS),
      null
    );
  }
}

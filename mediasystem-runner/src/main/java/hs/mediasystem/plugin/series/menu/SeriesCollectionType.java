package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.scanner.api.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesCollectionType implements CollectionType {
  private static final MediaType SERIE = MediaType.of("SERIE");

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", WorkBinder.BY_NAME),
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("watched-date", WorkBinder.BY_LAST_WATCHED_DATE.reversed())
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent()),
    new Filter<>("watched-recently", r -> r.getState().getLastWatchedTime().filter(d -> d.isAfter(Instant.now().minus(365 * 2, ChronoUnit.DAYS))).isPresent()
    )
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("unwatched", r -> !r.getState().isWatched())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private WorksService worksService;

  @Override
  public String getId() {
    return SERIE.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      worksService.findAllByType(SERIE, tag),
      "Series" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS),
      null
    );
  }
}

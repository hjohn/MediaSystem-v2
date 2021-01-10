package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.runner.grouping.GenreGrouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesCollectionType implements CollectionType {
  private static final Comparator<Object> BY_NAME = Comparator.comparing(SeriesCollectionType::extractDetails, Details.ALPHABETICAL);
  private static final Comparator<Object> BY_RELEASE_DATE_REVERSED = Comparator.comparing(SeriesCollectionType::extractDetails, Details.RELEASE_DATE_REVERSED).thenComparing(BY_NAME);
  private static final Comparator<Object> BY_LAST_WATCHED_REVERSED = Comparator.comparing(SeriesCollectionType::extractState, State.WATCHED_DATE_REVERSED).thenComparing(BY_NAME);
  private static final Comparator<Object> BY_RATING_REVERSED = Comparator.comparing(SeriesCollectionType::extractReception, Reception.RATING_REVERSED).thenComparing(BY_NAME);

  private static final List<SortOrder<Object>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", BY_NAME),
    new SortOrder<>("release-date", BY_RELEASE_DATE_REVERSED),
    new SortOrder<>("watched-date", BY_LAST_WATCHED_REVERSED),
    new SortOrder<>("rating", BY_RATING_REVERSED)
  );

  private static final List<Filter<Object>> FILTERS = List.of(
    new Filter<>("none", w -> true),
    new Filter<>("released-recently", w -> extractDetails(w).getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent()),
    new Filter<>("watched-recently", w -> extractState(w).getLastConsumptionTime().filter(d -> d.isAfter(Instant.now().minus(365 * 2, ChronoUnit.DAYS))).isPresent())
  );

  private static final List<Filter<Object>> STATE_FILTERS = List.of(
    new Filter<>("none", w -> true),
    new Filter<>("unwatched", w -> !extractState(w).isConsumed())
  );

  @Inject private GenericCollectionPresentationFactory factory;
  @Inject private GenreGrouping genreGrouper;
  @Inject private WorksClient worksClient;

  @Override
  public String getId() {
    return MediaType.SERIE.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      () -> worksClient.findAllByType(MediaType.SERIE, tag),
      "Series" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(
        SORT_ORDERS,
        FILTERS,
        STATE_FILTERS,
        List.of(genreGrouper, new NoGrouping<Work, Object>())
      ),
      null
    );
  }

  private static Details extractDetails(Object obj) {
    if(obj instanceof Work) {
      Work work = (Work)obj;

      return work.getDetails();
    }

    WorksGroup wg = (WorksGroup)obj;

    return wg.getDetails();
  }

  private static Reception extractReception(Object obj) {
    return extractDetails(obj).getReception().orElse(Reception.EMPTY);
  }

  private static State extractState(Object obj) {
    if(obj instanceof Work) {
      Work work = (Work)obj;

      return work.getState();
    }

    return State.EMPTY;
  }
}

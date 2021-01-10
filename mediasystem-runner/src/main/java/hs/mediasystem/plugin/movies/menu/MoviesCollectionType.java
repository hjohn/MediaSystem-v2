package hs.mediasystem.plugin.movies.menu;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.runner.grouping.CollectionGrouping;
import hs.mediasystem.runner.grouping.GenreGrouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.runner.grouping.WorksGroup;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesCollectionType implements CollectionType {
  private static final Comparator<Object> BY_NAME = Comparator.comparing(MoviesCollectionType::extractDetails, Details.ALPHABETICAL);
  private static final Comparator<Object> BY_RELEASE_DATE_REVERSED = Comparator.comparing(MoviesCollectionType::extractDetails, Details.RELEASE_DATE_REVERSED);

  private static final List<SortOrder<Object>> SORT_ORDERS = List.of(
    new SortOrder<>("alpha", BY_NAME),
    new SortOrder<>("release-date", BY_RELEASE_DATE_REVERSED, w -> List.of(extractDetails(w).getReleaseDate().map(LocalDate::getYear).map(Object::toString).orElse("Unknown")), true)
  );

  private static final List<Filter<Object>> FILTERS = List.of(
    new Filter<>("none", w -> true),
    new Filter<>("released-recently", w -> extractDetails(w).getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Object>> STATE_FILTERS = List.of(
    new Filter<>("none", w -> true),
    new Filter<>("unwatched", w -> !extractState(w).isConsumed())
  );

  @Inject private GenericCollectionPresentationFactory factory;
  @Inject private GenreGrouping genreGrouper;
  @Inject private CollectionGrouping collectionGrouper;
  @Inject private WorksClient worksClient;

  @Override
  public String getId() {
    return MediaType.MOVIE.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      () -> worksClient.findAllByType(MediaType.MOVIE, tag),
      "Movies" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(
        SORT_ORDERS,
        FILTERS,
        STATE_FILTERS,
        List.of(collectionGrouper, genreGrouper, new NoGrouping<Work, Object>())
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

  private static State extractState(Object obj) {
    if(obj instanceof Work) {
      Work work = (Work)obj;

      return work.getState();
    }

    return State.EMPTY;
  }
}

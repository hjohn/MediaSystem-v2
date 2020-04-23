package hs.mediasystem.plugin.movies.menu;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.runner.grouping.CollectionGrouping;
import hs.mediasystem.runner.grouping.GenreGrouping;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesCollectionType implements CollectionType {
  private static SortOrder<Work> ALPHABETICALLY = new SortOrder<>("alpha", WorkBinder.BY_NAME);

  private static SortOrder<Work> BY_RELEASE_DATE = new SortOrder<>(
    "release-date",
    WorkBinder.BY_REVERSE_RELEASE_DATE
//    new DelegatingComparator<>(mi -> mi.getData() instanceof GroupDescriptor, MediaItem.BY_PRODUCTION_NAME, Comparator.nullsLast(MediaItem.BY_PRODUCTION_RELEASE_DATE.reversed())),
//    mi -> List.of(mi.date.getValue().map(LocalDate::getYear).map(Object::toString).orElse("Unknown")),
//    true
  );

  private static SortOrder<Work> BY_GENRE = new SortOrder<>("genre", WorkBinder.BY_NAME, r -> r.getDetails().getClassification().getGenres(), false);

  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    ALPHABETICALLY,
    BY_RELEASE_DATE,
    BY_GENRE
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("unwatched", r -> !r.getState().isConsumed().getValue())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
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
      worksClient.findAllByType(MediaType.MOVIE, tag),
      "Movies" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(
        SORT_ORDERS,
        FILTERS,
        STATE_FILTERS,
        List.of(collectionGrouper, genreGrouper, new NoGrouping<Work>())
      ),
      null
    );
  }
}

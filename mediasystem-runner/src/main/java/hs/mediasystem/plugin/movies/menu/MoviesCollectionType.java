package hs.mediasystem.plugin.movies.menu;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.StateFilter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.ViewOptions;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.collection.CollectionType;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.runner.grouping.CollectionGrouping;
import hs.mediasystem.runner.grouping.GenreGrouping;
import hs.mediasystem.runner.grouping.GroupDescriptor;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.runner.util.DelegatingComparator;
import hs.mediasystem.scanner.api.MediaType;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesCollectionType implements CollectionType {
  private static final MediaType MOVIE = MediaType.of("MOVIE");

  private static SortOrder<MediaDescriptor> ALPHABETICALLY = new SortOrder<>("alpha", MediaItem.BY_NAME);

  private static SortOrder<MediaDescriptor> BY_RELEASE_DATE = new SortOrder<>(
    "release-date",
    new DelegatingComparator<>(mi -> mi.getData() instanceof GroupDescriptor, MediaItem.BY_NAME, Comparator.nullsLast(MediaItem.BY_RELEASE_DATE.reversed())),
    mi -> List.of(mi.date.getValue().map(LocalDate::getYear).map(Object::toString).orElse("Unknown")),
    true
  );

  private static SortOrder<MediaDescriptor> BY_GENRE = new SortOrder<>("genre", MediaItem.BY_NAME, mi -> mi.genres.getValue(), false);

  private static final List<SortOrder<MediaDescriptor>> SORT_ORDERS = List.of(
    ALPHABETICALLY,
    BY_RELEASE_DATE,
    BY_GENRE
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> mi.date.get().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private MediaService mediaService;
  @Inject private MediaItem.Factory mediaItemFactory;
  @Inject private GenreGrouping genreGrouper;
  @Inject private CollectionGrouping collectionGrouper;

  @Override
  public String getId() {
    return MOVIE.toString();
  }

  @Override
  public Presentation createPresentation(String tag) {
    return factory.create(
      createProductionItems(mediaService.findAllByType(MOVIE, tag, List.of("TMDB", "LOCAL"))),
      "Movies" + (tag == null ? "" : ":" + tag),
      new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED), List.of(collectionGrouper, genreGrouper, new NoGrouping<Production>())),
      null
    );
  }

  private List<MediaItem<Production>> createProductionItems(List<Production> descriptors) {
    return descriptors.stream().map(d -> mediaItemFactory.create(d, null)).collect(Collectors.toUnmodifiableList());
  }
}

package hs.mediasystem.plugin.movies.menu;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.StateFilter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.ViewOptions;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.runner.grouping.CollectionGrouping;
import hs.mediasystem.runner.grouping.GenreGrouping;
import hs.mediasystem.runner.grouping.GroupDescriptor;
import hs.mediasystem.runner.grouping.NoGrouping;
import hs.mediasystem.runner.util.DelegatingComparator;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesPlugin implements Plugin {
  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final Comparator<MediaItem<MediaDescriptor>> RELEASE_DATE_COMPARATOR = Comparator.comparing((MediaItem<MediaDescriptor> mi) -> mi.date.get(), Comparator.nullsLast(Comparator.naturalOrder())).reversed();
  private static final Comparator<MediaItem<MediaDescriptor>> ALPHA_COMPARATOR = Comparator.comparing(MediaItem::getDetails, Comparator.comparing(Details::getName, NaturalLanguage.ALPHABETICAL));

  private static SortOrder<MediaDescriptor> ALPHABETICALLY = new SortOrder<>("alpha", ALPHA_COMPARATOR);

  private static SortOrder<MediaDescriptor> BY_RELEASE_DATE = new SortOrder<>(
    "release-date",
    new DelegatingComparator<>(mi -> mi.getData() instanceof GroupDescriptor, ALPHA_COMPARATOR, RELEASE_DATE_COMPARATOR),
    mi -> List.of("" + mi.date.getValue().getYear()),
    true
  );

  private static SortOrder<MediaDescriptor> BY_GENRE = new SortOrder<>("genre", ALPHA_COMPARATOR, mi -> mi.genres.getValue(), false);

  private static final List<SortOrder<MediaDescriptor>> SORT_ORDERS = List.of(
    ALPHABETICALLY,
    BY_RELEASE_DATE,
    BY_GENRE
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> Optional.ofNullable(mi.date.get()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private MediaService mediaService;
  @Inject private MediaItem.Factory mediaItemFactory;
  @Inject private GenreGrouping genreGrouper;
  @Inject private CollectionGrouping collectionGrouper;

  @Override
  public Menu getMenu() {
    return new Menu("Movies", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Movies", null, this::createPresentation)
    ));
  }

  private GenericCollectionPresentation<Production> createPresentation() {
    return factory.create(
      createProductionItems(mediaService.findAllByType(MOVIE, List.of("TMDB", "LOCAL"))),
      "Movies",
      new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED), List.of(collectionGrouper, genreGrouper, new NoGrouping<Production>())),
      null
    );
  }

  private List<MediaItem<Production>> createProductionItems(List<Production> descriptors) {
    return descriptors.stream().map(d -> mediaItemFactory.create(d, null)).collect(Collectors.toUnmodifiableList());
  }
}

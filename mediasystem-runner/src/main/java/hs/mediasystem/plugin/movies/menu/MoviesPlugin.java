package hs.mediasystem.plugin.movies.menu;

import hs.mediasystem.ext.basicmediatypes.domain.CollectionDetails;
import hs.mediasystem.ext.basicmediatypes.domain.DetailedMediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.mediamanager.db.VideoDatabase;
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
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesPlugin implements Plugin {
  private static final MediaType MOVIE = MediaType.of("MOVIE");

  private static SortOrder<DetailedMediaDescriptor> ALPHABETICALLY = new SortOrder<>(
    "alpha",
    Comparator.comparing(MediaItem::getDetails, Comparator.comparing(Details::getName, NaturalLanguage.ALPHABETICAL))
  );

  private static SortOrder<DetailedMediaDescriptor> BY_RELEASE_DATE = new SortOrder<>(
    "release-date",
    Comparator.comparing((MediaItem<DetailedMediaDescriptor> mi) -> mi.date.get(), Comparator.nullsLast(Comparator.naturalOrder())).reversed(),
    mi -> List.of("" + mi.date.getValue().getYear()),
    true
  );

  private static SortOrder<DetailedMediaDescriptor> BY_GENRE = new SortOrder<>(
    "genre",
    Comparator.comparing(MediaItem::getDetails, Comparator.comparing(Details::getName, NaturalLanguage.ALPHABETICAL)),
    mi -> mi.genres.getValue(),
    false
  );

  private static final List<SortOrder<DetailedMediaDescriptor>> SORT_ORDERS = List.of(
    ALPHABETICALLY,
    BY_RELEASE_DATE,
    BY_GENRE
  );

  private static final List<Filter<DetailedMediaDescriptor>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> Optional.ofNullable(mi.date.get()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<DetailedMediaDescriptor>> GROUPS = List.of(
    new Filter<>("grouped", mi -> mi.getData() instanceof Movie ? ((Movie)mi.getData()).getCollectionDetails() == null : true),
    new Filter<>("ungrouped", mi -> !(mi.getData() instanceof ProductionCollection))
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private MediaService mediaService;
  @Inject private MediaItem.Factory mediaItemFactory;
  @Inject private VideoDatabase videoDatabase;

  @Override
  public Menu getMenu() {
    return new Menu("Movies", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Movies", null, this::createPresentation)
    ));
  }

  private GenericCollectionPresentation<DetailedMediaDescriptor> createPresentation() {
    ObservableList<MediaItem<Movie>> productionItems = createProductionItems(mediaService.findAllByType(MOVIE, List.of("TMDB", "LOCAL")));

    List<MediaItem<DetailedMediaDescriptor>> groups = productionItems.stream()
      .map(mi -> mi.getData().getCollectionDetails())
      .filter(Objects::nonNull)
      .map(CollectionDetails::getIdentifier)
      .distinct()
      .map(videoDatabase::queryProductionCollection)
      .map(DetailedMediaDescriptor.class::cast)
      .map(pc -> mediaItemFactory.create(pc, null))
      .collect(Collectors.toList());

    @SuppressWarnings("unchecked")
    ObservableList<MediaItem<DetailedMediaDescriptor>> items = (ObservableList<MediaItem<DetailedMediaDescriptor>>)(ObservableList<?>)productionItems;

    items.addAll(groups);

    return factory.create(items, "Movies", new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED), GROUPS));
  }

  private ObservableList<MediaItem<Movie>> createProductionItems(List<Movie> descriptors) {
    return FXCollections.observableArrayList(descriptors.stream().map(d -> mediaItemFactory.create(d, null)).collect(Collectors.toList()));
  }
}

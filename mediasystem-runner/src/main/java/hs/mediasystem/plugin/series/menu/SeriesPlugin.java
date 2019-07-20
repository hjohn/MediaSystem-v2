package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.StateFilter;
import hs.mediasystem.plugin.library.scene.view.GridViewPresentation.ViewOptions;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.CollectionLocationManager;
import hs.mediasystem.runner.CollectionLocationManager.CollectionDefinition;
import hs.mediasystem.runner.db.MediaService;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.NaturalLanguage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesPlugin implements Plugin {
  private static final MediaType SERIE = MediaType.of("SERIE");

  private static final List<SortOrder<Serie>> SORT_ORDERS = List.of(
    new SortOrder<Serie>("alpha", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getName, NaturalLanguage.ALPHABETICAL))),
    new SortOrder<Serie>("release-date", Comparator.comparing(MediaItem::getProduction, Comparator.comparing(Production::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())),
    new SortOrder<Serie>("watched-date", Comparator.comparing(mi -> mi.lastWatchedTime.get(), Comparator.<LocalDateTime>nullsFirst(Comparator.naturalOrder()).reversed()))
  );

  private static final List<Filter<Serie>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> Optional.ofNullable(mi.getProduction().getDate()).filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent()),
    new Filter<>("watched-recently", mi -> Optional.ofNullable(mi.lastWatchedTime.get()).filter(d -> d.isAfter(LocalDateTime.now().minusYears(2))).isPresent())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private CollectionLocationManager manager;
  @Inject private MediaService mediaService;
  @Inject private MediaItem.Factory mediaItemFactory;

  @Override
  public Menu getMenu() {
    List<MenuItem> menuItems = new ArrayList<>();

    for(CollectionDefinition collectionDefinition : manager.getCollectionDefinitions("Serie")) {
      menuItems.add(new MenuItem(collectionDefinition.getTitle(), null, () -> createPresentation(collectionDefinition.getTag())));
    }

    return new Menu("Series", ResourceManager.getImage(getClass(), "image"), menuItems);
  }

  private GenericCollectionPresentation<Serie> createPresentation(String tag) {
    return factory.create(
      createProductionItems(mediaService.findAllByType(SERIE, tag, List.of("TMDB", "LOCAL"))),
      "Series:" + tag,
      new ViewOptions<>(SORT_ORDERS, FILTERS, List.of(StateFilter.ALL, StateFilter.UNWATCHED)),
      null
    );
  }

  private ObservableList<MediaItem<Serie>> createProductionItems(List<Serie> descriptors) {
    return FXCollections.observableArrayList(descriptors.stream().map(d -> mediaItemFactory.create(d, null)).collect(Collectors.toList()));
  }
}

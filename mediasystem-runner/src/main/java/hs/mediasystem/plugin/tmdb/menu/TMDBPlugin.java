package hs.mediasystem.plugin.tmdb.menu;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
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
import hs.mediasystem.runner.util.ResourceManager;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TMDBPlugin implements Plugin {
  private static final List<SortOrder<MediaDescriptor>> SORT_ORDERS = List.of(
    new SortOrder<>("release-date", MediaItem.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", MediaItem.BY_NAME)
  );

  private static final List<Filter<Production>> FILTERS = List.of(
    new Filter<>("none", mi -> true),
    new Filter<>("released-recently", mi -> mi.date.get().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<StateFilter> STATE_FILTERS = List.of(StateFilter.ALL, StateFilter.AVAILABLE, StateFilter.UNWATCHED);

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private VideoDatabase videoDatabase;
  @Inject private MediaItem.Factory mediaItemFactory;

  @Override
  public Menu getMenu() {
    return new Menu("TMDB", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Top 100", null, () -> factory.create(
        createProductionItems(), "Top100", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null
      )),
      new MenuItem("Recommendations", null, () -> factory.create(
        createProductionItems(), "Recommended", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null
      ))
    ));
  }

  private ObservableList<MediaItem<Production>> createProductionItems() {
    List<Production> list = videoDatabase.queryTop100();

    return FXCollections.observableArrayList(list.stream().map(p -> mediaItemFactory.create(
      p,
      null
    )).collect(Collectors.toList()));
  }
}

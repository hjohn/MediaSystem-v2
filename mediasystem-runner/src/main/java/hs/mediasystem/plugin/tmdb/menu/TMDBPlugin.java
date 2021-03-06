package hs.mediasystem.plugin.tmdb.menu;

import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentation.ViewOptions;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TMDBPlugin implements Plugin {
  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("release-date", WorkBinder.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", WorkBinder.BY_NAME)
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("available", r -> r.getPrimaryStream().isPresent()),
    new Filter<>("unwatched", r -> r.getPrimaryStream().isPresent() && !r.getState().isConsumed().getValue())
  );

  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private WorksClient worksClient;

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

  private ObservableList<Work> createProductionItems() {
    return FXCollections.observableArrayList(worksClient.findTop100());
  }
}

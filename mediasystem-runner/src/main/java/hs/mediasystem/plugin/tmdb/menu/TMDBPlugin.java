package hs.mediasystem.plugin.tmdb.menu;

import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.Filter;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.SortOrder;
import hs.mediasystem.plugin.library.scene.grid.GridViewPresentationFactory.ViewOptions;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.ui.api.WorksClient;
import hs.mediasystem.ui.api.domain.Work;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TMDBPlugin implements Plugin {
  private static final List<SortOrder<Work>> SORT_ORDERS = List.of(
    new SortOrder<>("release-date", Work.BY_RELEASE_DATE.reversed()),
    new SortOrder<>("alpha", Work.BY_NAME)
  );

  private static final List<Filter<Work>> FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("released-recently", r -> r.getDetails().getReleaseDate().filter(d -> d.isAfter(LocalDate.now().minusYears(5))).isPresent())
  );

  private static final List<Filter<Work>> STATE_FILTERS = List.of(
    new Filter<>("none", r -> true),
    new Filter<>("available", r -> r.getPrimaryStream().isPresent()),
    new Filter<>("unwatched", r -> r.getPrimaryStream().isPresent() && !r.getState().isConsumed())
  );

  @Inject private GenericCollectionPresentationFactory factory;
  @Inject private WorksClient worksClient;

  @Override
  public Menu getMenu() {
    return new Menu("TMDB", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Top 100", null, () -> factory.create(
        () -> createProductionItems(), "Top100", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null, Work::getId
      )),
      new MenuItem("Recommendations", null, () -> factory.create(
        () -> createProductionItems(), "Recommended", new ViewOptions<>(SORT_ORDERS, FILTERS, STATE_FILTERS), null, Work::getId
      ))
    ));
  }

  private List<Work> createProductionItems() {
    return worksClient.findTop100();
  }
}

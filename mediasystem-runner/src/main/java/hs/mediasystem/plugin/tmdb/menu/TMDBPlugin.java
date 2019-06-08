package hs.mediasystem.plugin.tmdb.menu;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TMDBPlugin implements Plugin {
  @Inject private GenericCollectionPresentation.Factory factory;
  @Inject private VideoDatabase videoDatabase;
  @Inject private MediaItem.Factory mediaItemFactory;

  @Override
  public Menu getMenu() {
    return new Menu("TMDB", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Top 100", null, () -> factory.create(createProductionItems(), "Top100")),
      new MenuItem("Recommendations", null, () -> factory.create(createProductionItems(), "Recommended"))
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

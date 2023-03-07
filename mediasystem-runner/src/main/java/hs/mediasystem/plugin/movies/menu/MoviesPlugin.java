package hs.mediasystem.plugin.movies.menu;

import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.resource.ResourceManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesPlugin implements Plugin {
  @Inject private MoviesCollectionType collectionType;

  @Override
  public Menu getMenu() {
    return new Menu("Movies", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Movies", null, () -> collectionType.createPresentation(null))
    ));
  }
}

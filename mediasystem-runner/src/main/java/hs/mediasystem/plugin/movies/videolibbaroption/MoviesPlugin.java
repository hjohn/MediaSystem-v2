package hs.mediasystem.plugin.movies.videolibbaroption;

import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MoviesPlugin implements Plugin {
  @Inject private MovieCollectionPresentation.Factory movieCollectionPresentationFactory;

  @Override
  public Menu getMenu() {
    return new Menu("Movies", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Movies", null, () -> movieCollectionPresentationFactory.create())
    ));
  }
}

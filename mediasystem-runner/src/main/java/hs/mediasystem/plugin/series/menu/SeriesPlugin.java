package hs.mediasystem.plugin.series.menu;

import hs.mediasystem.domain.work.Collection;
import hs.mediasystem.domain.work.CollectionDefinition;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.ResourceManager;
import hs.mediasystem.ui.api.CollectionClient;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SeriesPlugin implements Plugin {
  @Inject private SeriesCollectionType collectionType;
  @Inject private CollectionClient collectionClient;

  @Override
  public Menu getMenu() {
    List<MenuItem> menuItems = new ArrayList<>();

    for(Collection collection : collectionClient.findCollections()) {
      CollectionDefinition collectionDefinition = collection.getDefinition();

      if(collectionDefinition.getType().equalsIgnoreCase("serie")) {
        menuItems.add(new MenuItem(collectionDefinition.getTitle(), null, () -> collectionType.createPresentation(collectionDefinition.getTag())));
      }
    }

    return new Menu("Series", ResourceManager.getImage(getClass(), "image"), menuItems);
  }
}

package hs.mediasystem.plugin.settings.rootmenu;

import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class SettingsPlugin implements Plugin {

  @Override
  public Menu getMenu() {
    return new Menu("Settings", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Settings", null, () -> null)
    ));
  }
}

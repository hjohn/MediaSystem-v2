package hs.mediasystem.plugin.settings.menu;

import hs.mediasystem.db.ScannerController;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.Plugin;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.runner.util.ResourceManager;

import java.util.List;

import javafx.scene.control.Label;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SettingsPlugin implements Plugin {
  @Inject private ScannerController scannerController;

  @Override
  public Menu getMenu() {
    return new Menu("Settings", ResourceManager.getImage(getClass(), "image"), List.of(
      new MenuItem("Settings", null, () -> null),
      new MenuItem("Rescan Library", null, e -> {
        scannerController.scanNow();
        Dialogs.show(e, new Label("Scan initiated..."), "Ok");
      })
    ));
  }
}

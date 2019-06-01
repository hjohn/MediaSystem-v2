package hs.mediasystem.plugin.settings.rootmenu;

import hs.mediasystem.plugin.rootmenu.RootMenuScenePlugin;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

import javax.inject.Singleton;

@Singleton
public class Settings implements RootMenuScenePlugin.MenuPlugin {

  public interface Plugin extends hs.mediasystem.runner.PluginBase {
    String getKey();
  }

  @Override
  public Node getNode() {
    return new Button(getText("title"), new ImageView(getImage("image")));
  }
}

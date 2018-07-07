package hs.mediasystem.plugin.settings.rootmenu;

import hs.mediasystem.plugin.rootmenu.RootMenuScenePlugin;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class Settings implements RootMenuScenePlugin.MenuPlugin {

  public interface Plugin extends hs.mediasystem.runner.PluginBase {
    String getKey();
  }

  @Inject private Provider<List<Plugin>> pluginsProvider;

  @Override
  public Node getNode() {
    return new Button(getText("title"), new ImageView(getImage("image")));
  }
}

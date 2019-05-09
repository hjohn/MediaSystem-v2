package hs.mediasystem.plugin.videolibbar.rootmenu;

import hs.mediasystem.plugin.rootmenu.RootMenuScenePlugin;
import hs.mediasystem.runner.PluginBase;

import java.util.Comparator;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class VideoLibraryMenuPlugin implements RootMenuScenePlugin.MenuPlugin {

  public interface OptionPlugin extends PluginBase {
    void onSelect(ActionEvent e);
  }

  @Inject private Provider<List<OptionPlugin>> pluginsProvider;

  @Override
  public Node getNode() {
    VBox vbox = new VBox();

    pluginsProvider.get().stream()
      .sorted(Comparator.comparing(p -> p.getDouble("order")))
      .map(this::toButton)
      .forEach(b -> vbox.getChildren().add(b));

    return vbox;
  }

  private Button toButton(OptionPlugin plugin) {
    Button button = new Button(plugin.getText("title"), new ImageView(plugin.getImage("image")));

    button.setOnAction(plugin::onSelect);

    return button;
  }
}

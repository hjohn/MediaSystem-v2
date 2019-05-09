package hs.mediasystem.plugin.rootmenu;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.PluginBase;
import hs.mediasystem.util.FocusEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class RootMenuScenePlugin2 implements NodeFactory<MenuPresentation> {
  public interface MenuPlugin extends PluginBase {
    Node getNode();
  }

  @Inject private Provider<List<MenuPlugin>> pluginsProvider;

  @Override
  public Node create(MenuPresentation presentation) {
    List<Node> nodes = pluginsProvider.get().stream()
      .sorted(Comparator.comparing(p -> p.getDouble("order")))
      .map(this::toBar)
      .collect(Collectors.toList());

    VBox vbox = new VBox();

    vbox.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
    vbox.getStyleClass().add("menu-scroll-box");

    vbox.getChildren().setAll(nodes);

    vbox.addEventHandler(FocusEvent.FOCUS_GAINED, new EventHandler<FocusEvent>() {
      @Override
      public void handle(FocusEvent event) {
        Node target = ((Node)event.getTarget());

        Platform.runLater(() -> {  // During construction sizes are still unknown.
          double sceneHeight = vbox.getScene().getHeight();

          Bounds bounds = target.getLayoutBounds();
          Point2D p2d = target.localToScene((bounds.getMinX() + bounds.getMaxX()) / 2, (bounds.getMinY() + bounds.getMaxY()) / 2);

          vbox.setTranslateX(vbox.getScene().getWidth() / 2);

          p2d = p2d.add(0, -sceneHeight / 2);
          p2d = vbox.sceneToLocal(p2d);

          new Timeline(new KeyFrame(Duration.millis(250),
            new KeyValue(vbox.translateYProperty(), -p2d.getY())
          )).play();
        });
      }
    });

    return vbox;
  }

  private Node toBar(MenuPlugin plugin) {
    VBox vbox = new VBox();

    Label label = new Label(plugin.getText("title"));

    Node node = plugin.getNode();

    vbox.getChildren().addAll(label, node);
/*
    node.addEventHandler(FocusEvent.FOCUS_GAINED, new EventHandler<FocusEvent>() {
      @Override
      public void handle(FocusEvent event) {
        Node target = ((Node)event.getTarget());

        new Timeline(new KeyFrame(Duration.millis(250),
          new KeyValue(node.translateXProperty(), -target.getLayoutX())
        )).play();
      }
    });
*/
    return vbox;
  }
}

package hs.mediasystem.plugin.rootmenu;

import hs.mediasystem.plugin.rootmenu.MenuPresentation.Menu;
import hs.mediasystem.plugin.rootmenu.MenuPresentation.MenuItem;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.util.FocusEvent;
import hs.mediasystem.util.javafx.control.Buttons;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import javax.inject.Singleton;

import org.reactfx.util.Interpolator;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

@Singleton
public class RootMenuScenePlugin implements NodeFactory<MenuPresentation> {

  @Override
  public Node create(MenuPresentation presentation) {
    HBox hbox = new HBox();

    hbox.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
    hbox.getStyleClass().add("menu-scroll-box");

    for(Menu menu : presentation.menus) {
      MenuPane pane = new MenuPane(menu.getImage());

      for(MenuItem menuItem : menu.getMenuItems()) {
        pane.getChildren().add(Buttons.create(menuItem.getTitle(), menuItem.getEventConsumer()::accept));
      }

      hbox.getChildren().add(pane);
    }

    hbox.addEventHandler(FocusEvent.FOCUS_GAINED, new EventHandler<FocusEvent>() {
      @Override
      public void handle(FocusEvent event) {
        Node target = ((Node)event.getTarget());

        Platform.runLater(() -> {  // During construction sizes are still unknown.
          double scenewidth = hbox.getScene().getWidth();

          Bounds bounds = target.getLayoutBounds();
          Point2D p2d = target.localToScene((bounds.getMinX() + bounds.getMaxX()) / 2, (bounds.getMinY() + bounds.getMaxY()) / 2);

          //hbox.setTranslateX(hbox.getScene().getWidth() / 2);

          p2d = p2d.add(-scenewidth / 2, 0);
          p2d = hbox.sceneToLocal(p2d);

//          new Timeline(new KeyFrame(Duration.millis(250),
//            new KeyValue(hbox.translateXProperty(), -p2d.getX())
//          )).play();
        });
      }
    });

    return hbox;
  }

  public class MenuPane extends Pane {
    private final Label imageLabel;

    private final Val<Double> animatedActiveIndex;
    private final Var<Double> activeIndex = Var.newSimpleVar(0.0);

    public MenuPane(Image image) {
      imageLabel = new Label("", new ImageView(image));

      getChildren().add(imageLabel);

      animatedActiveIndex = activeIndex.animate(java.time.Duration.ofMillis(250), Interpolator.LINEAR_DOUBLE);
      animatedActiveIndex.observeChanges((obs, old, current) -> requestLayout());
    }

    @Override
    protected void layoutChildren() {
      super.layoutChildren();

      double ih = imageLabel.prefHeight(-1);

      double w = getWidth();
      double x = 0;
      double h = 50;

      ObservableList<Node> children = getChildren();

      layoutInArea(imageLabel, x, (getHeight() - ih) / 2, w, ih, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

      Node focusedNode = children.stream().filter(Node::isFocused).findFirst().orElse(null);

      int focusedIndex = children.indexOf(focusedNode);

      if(focusedIndex != -1) {
        activeIndex.setValue((double)focusedIndex - 1);
      }

      double index = animatedActiveIndex.getValue();

      for(int i = 0; i < children.size() - 1; i++) {
        double y = 0.5 * getHeight() + h * 0.5;

        if(Math.floor(index) == i) {
          y += 0.5 * (ih - h) + (ih + h) * (i - index);
        }
        else {
          y += (i - index - 0.5) * h;

          if(i < index) {
            y -= 0.5 * ih;
          }
          else {
            y += 0.5 * ih;
          }
        }

        Node node = children.get(i + 1);

        layoutInArea(node, x, y, w, h, 0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER);

        y += h;
      }
    }
  }
}


package hs.mediasystem.plugin.library.scene.base;

import hs.mediasystem.presentation.ViewPortFactory;
import hs.mediasystem.util.javafx.control.GridPaneUtil;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * The CollectionView provides a standard area with a background and a small bottom border in which
 * information about a collection of media can be shown.  How the collection is shown is determined by
 * the current layout.<p>
 *
 * Provided properties, events and methods:
 * - the MediaRoot under which the media items to be displayed are located
 * - the currently focused media item
 * - the layout to use
 * - the group set to use
 * - an event handler triggered when a node is selected
 * - an event handler triggered when options is chosen
 * - a method for selecting a node by id
 */
public class EntityView extends StackPane {
  public final BackgroundPane backgroundPane = new BackgroundPane();
  public final ObjectProperty<Node> defaultInputFocus = new SimpleObjectProperty<>();

  private final GridPane content = GridPaneUtil.create(new double[] {20, 30, 30, 20}, new double[] {20, 30, 41, 9});

  private StackPane center;
  private StackPane statusArea = new StackPane();

  public EntityView(ViewPortFactory viewPortFactory, LibraryPresentation entityPresentation) {
    GridPane stage = GridPaneUtil.create(new double[] {100}, new double[] {90, 10});

    stage.add(backgroundPane, 0, 0);
    stage.add(new StackPane() {{
      getStyleClass().add("stage");
      getChildren().add(statusArea);
    }}, 0, 1);

    center = viewPortFactory.create(entityPresentation);

    setupForegroundFade();

    getChildren().addAll(stage, content, center);
  }

  private void setupForegroundFade() {
    Timeline timeline = new Timeline(
      new KeyFrame(Duration.seconds(0.5), new KeyValue(center.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(30), new KeyValue(center.opacityProperty(), 1.0)),
      new KeyFrame(Duration.seconds(37), new KeyValue(center.opacityProperty(), 0.0))
    );

    timeline.playFromStart();

    this.addEventFilter(KeyEvent.ANY, e -> {
      if(timeline.getCurrentTime().greaterThan(Duration.seconds(30))) {
        e.consume();  // consume first key event when fade out started
      }

      timeline.playFromStart();
    });
  }

  public void setCenter(Node node) {
    center.getChildren().setAll(node);
  }

  public void setStatusArea(Node node) {
    if(node == null) {
      statusArea.getChildren().clear();
    }
    else {
      statusArea.getChildren().setAll(node);
    }
  }

  @Override
  public void requestFocus() {
    if(defaultInputFocus.get() != null) {
      Platform.runLater(() -> {
        Node node = defaultInputFocus.get();

        if(node != null) {
          node.requestFocus();
        }
      });
    }
  }
}

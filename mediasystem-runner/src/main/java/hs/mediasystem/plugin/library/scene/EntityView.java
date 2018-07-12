package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.presentation.ViewPortFactory;
import hs.mediasystem.util.javafx.GridPaneUtil;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

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

  public EntityView(ViewPortFactory viewPortFactory, LibraryPresentation entityPresentation) {
    GridPane stage = GridPaneUtil.create(new double[] {100}, new double[] {90, 10});

    stage.add(backgroundPane, 0, 0);
    stage.add(new StackPane() {{
      getStyleClass().add("stage");
    }}, 0, 1);

    center = viewPortFactory.create(entityPresentation);

    getChildren().addAll(stage, content, center);
  }

  public void setCenter(Node node) {
    center.getChildren().setAll(node);
  }

  @Override
  public void requestFocus() {
    if(defaultInputFocus.get() != null) {
      Platform.runLater(() -> defaultInputFocus.get().requestFocus());  // TODO make null check part of this
    }
  }
}

package hs.mediasystem.util.javafx;

import java.util.Optional;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;
import javafx.stage.Window;

public class Nodes {

  public static ObservableValue<Boolean> showing(Node node) {
    return node.sceneProperty()
      .flatMap(Scene::windowProperty)
      .flatMap(Window::showingProperty)
      .orElse(false);
  }

  public static boolean isTreeVisibleAndShowing(Node node) {
    return node.getScene() != null && node.getScene().getWindow() != null && node.getScene().getWindow().isShowing() && isTreeVisible(node);
  }

  public static boolean isTreeVisible(Node node) {
    return node.isVisible() && (node.getParent() == null || isTreeVisible(node.getParent()));
  }

  private static void bindVisibilityEvents(Node node, Runnable whenShown, Runnable whenHidden) {
    showing(node).addListener((obs, old, visible) -> {
      if(visible) {
        whenShown.run();
      }
      else {
        whenHidden.run();
      }
    });
  }

  public static <T> void safeBindBidirectionalSelectedItemToModel(ListView<T> view, Property<T> modelProperty) {
    SelectionModel<T> selectionModel = view.getSelectionModel();

    ChangeListener<T> updatePresentation = (obs, old, current) -> modelProperty.setValue(current);

    ChangeListener<T> updateView = (obs, old, current) -> {
      if(!selectionModel.getSelectedItem().equals(current)) {
        selectionModel.select(current);
      }
    };

    Nodes.bindVisibilityEvents(
      view,
      () -> {  // when shown
        selectionModel.select(modelProperty.getValue());
        selectionModel.selectedItemProperty().addListener(updatePresentation);
        modelProperty.addListener(updateView);
      },
      () -> {  // when hidden
        selectionModel.selectedItemProperty().removeListener(updatePresentation);
        modelProperty.removeListener(updateView);
      }
    );
  }

  /**
   * Determines if the given {@link Node} or any child node has focus.
   *
   * @param node a {@link Node} under which to check for a focused node
   * @return the {@link Node} which has focus, or empty otherwise
   */
  public static Optional<Node> findFocusedNodeInTree(Node node) {
    Scene scene = node.getScene();

    if(scene == null) {
      return Optional.empty();
    }

    Node focusOwner = node.getScene().getFocusOwner();

    while(focusOwner != null) {
      if(focusOwner.equals(node)) {
        return Optional.of(node.getScene().getFocusOwner());
      }

      focusOwner = focusOwner.getParent();
    }

    return Optional.empty();
  }

  /**
   * Focuses the first focusable node under the given node.
   *
   * @param node a {@link Node} or child of the given node to give focus.
   * @return <code>true</code> if focus was given, otherwise <code>false</code>
   */
  public static boolean focus(Node node) {
    if(node instanceof Parent) {
      for(Node child : ((Parent)node).getChildrenUnmodifiable()) {
        if(focus(child)) {
          return true;
        }
      }
    }

    if(node.isFocusTraversable() && !node.isDisabled() && node.isVisible()) {
      node.requestFocus();
      return true;
    }

    return false;
  }
}

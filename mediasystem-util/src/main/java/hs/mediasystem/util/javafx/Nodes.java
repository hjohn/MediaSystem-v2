package hs.mediasystem.util.javafx;

import hs.jfx.eventstream.ValueStream;
import hs.jfx.eventstream.Values;

import java.util.Optional;

import javafx.beans.binding.Binding;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionModel;

public class Nodes {

  public static Binding<Boolean> showing(Node node) {
    return showingStream(node).toBinding();
  }

  public static ValueStream<Boolean> showingStream(Node node) {
    return Values.of(node.sceneProperty())
      .flatMap(s -> Values.of(s.windowProperty()))
      .flatMap(w -> Values.of(w.showingProperty()))
      .orElse(false);
  }

  public static boolean isTreeVisibleAndShowing(Node node) {
    return node.getScene() != null && node.getScene().getWindow() != null && node.getScene().getWindow().isShowing() && isTreeVisible(node);
  }

  public static boolean isTreeVisible(Node node) {
    return node.isVisible() && (node.getParent() == null || isTreeVisible(node.getParent()));
  }

  public static <T> void listenWhenNodeVisible(Node node, ObservableValue<T> property, ChangeListener<? super T> listener) {
    showing(node).addListener((obs, old, visible) -> {
      if(visible) {
        property.addListener(listener);
      }
      else {
        property.removeListener(listener);
      }
    });
  }

  public static void bindVisibilityEvents(Node node, Runnable whenShown, Runnable whenHidden) {
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
   * @return <code>true</code> if this part of the tree has the focus, otherwise <code>false</code>
   */
  public static Optional<Node> findFocusedNodeInTree(Node node) {
    if(node instanceof Parent) {
      for(Node child : ((Parent)node).getChildrenUnmodifiable()) {
        Optional<Node> focusedNode = findFocusedNodeInTree(child);

        if(focusedNode.isPresent()) {
          return focusedNode;
        }
      }
    }

    return node.isFocusTraversable() && !node.isDisabled() && node.isFocused() ? Optional.of(node) : Optional.empty();
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

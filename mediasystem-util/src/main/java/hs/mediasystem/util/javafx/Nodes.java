package hs.mediasystem.util.javafx;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

import org.reactfx.value.Val;

public class Nodes {
  public static Val<Boolean> visible(Node node) {
    return Val.flatMap(node.sceneProperty(), Scene::windowProperty)
      .flatMap(Window::showingProperty)
      .orElseConst(false);
  }

  public static boolean isTreeVisibleAndShowing(Node node) {
    return node.getScene() != null && node.getScene().getWindow() != null && node.getScene().getWindow().isShowing() && isTreeVisible(node);
  }

  public static boolean isTreeVisible(Node node) {
    return node.isVisible() && (node.getParent() == null || isTreeVisible(node.getParent()));
  }
}

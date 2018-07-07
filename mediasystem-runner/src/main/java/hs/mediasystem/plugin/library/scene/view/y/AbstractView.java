package hs.mediasystem.plugin.library.scene.view.y;

import javafx.scene.Node;

public abstract class AbstractView implements View {
  private final Node node;

  public AbstractView(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return node;
  }
}

package hs.mediasystem.plugin.library.scene.view.x;

import javafx.scene.Node;

public class Fragment<P> {
  private final Node node;
  private final P presentation;

  public Fragment(Node node, P presentation) {
    this.node = node;
    this.presentation = presentation;
  }

  public Node getNode() {
    return node;
  }

  public P getPresentation() {
    return presentation;
  }
}

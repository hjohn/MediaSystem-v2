package hs.mediasystem.presentation;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class ViewPort extends TransitionPane {
  private final ParentPresentation parentPresentation;
  private final Consumer<Node> nodeAdjuster;

  public ViewPort(Theme theme, ParentPresentation parentPresentation, Consumer<Node> nodeAdjuster) {
    super(new TransitionPane.FadeIn(), null);

    this.parentPresentation = parentPresentation;
    this.nodeAdjuster = nodeAdjuster;

    ChangeListener<? super Presentation> listener = (obs, old, current) -> updateChildNode(theme, current);

    parentPresentation.childPresentation.addListener(listener);

    updateChildNode(theme, parentPresentation.childPresentation.get());
  }

  protected Node updateChildNode(Theme theme, Presentation current) {
    if(current == null) {
      return null;
    }

    Node node = theme.findPlacer(parentPresentation, current).place(parentPresentation, current);

    node.getProperties().put("presentation2", current);

    add((Pane)node);

    if(nodeAdjuster != null) {
      nodeAdjuster.accept(node);
    }

    return node;
  }
}

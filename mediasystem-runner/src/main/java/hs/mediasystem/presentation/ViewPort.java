package hs.mediasystem.presentation;

import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.ui.transition.StandardTransitions;
import hs.mediasystem.util.javafx.ui.transition.TransitionPane;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;

public class ViewPort extends TransitionPane {
  private final ParentPresentation parentPresentation;
  private final Consumer<Node> nodeAdjuster;

  public ViewPort(Theme theme, ParentPresentation parentPresentation, Consumer<Node> nodeAdjuster) {
    super(StandardTransitions.fade());

    this.parentPresentation = parentPresentation;
    this.nodeAdjuster = nodeAdjuster;

    ChangeListener<? super Presentation> listener = (obs, old, current) -> updateChildNode(theme, current);

    parentPresentation.childPresentation.conditionOn(Nodes.showing(this)).addListener(listener);

    updateChildNode(theme, parentPresentation.childPresentation.get());
  }

  protected Node updateChildNode(Theme theme, Presentation current) {
    if(current == null) {
      return null;
    }

    Node node = theme.findPlacer(parentPresentation, current).place(parentPresentation, current);

    node.getProperties().put("presentation2", current);

    add(node);  // after transition ends, node will be the single visible node

    if(nodeAdjuster != null) {
      nodeAdjuster.accept(node);
    }

    return node;
  }
}

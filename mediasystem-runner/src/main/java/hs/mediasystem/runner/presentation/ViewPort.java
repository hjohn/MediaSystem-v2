package hs.mediasystem.runner.presentation;

import hs.mediasystem.presentation.Navigable;
import hs.mediasystem.presentation.NavigateEvent;
import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.ui.transition.StandardTransitions;
import hs.mediasystem.util.javafx.ui.transition.TransitionPane;

import java.util.function.Consumer;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;

public class ViewPort extends TransitionPane {
  private final Theme theme;
  private final Consumer<Node> nodeAdjuster;

  public static ViewPort ofPresentation(Theme theme, ParentPresentation presentation, Consumer<Node> nodeAdjuster) {
    return new ViewPort(theme, presentation, nodeAdjuster);
  }

  public static ViewPort fixed(Theme theme, Presentation presentation, Consumer<Node> nodeAdjuster) {
    return new ViewPort(theme, presentation, nodeAdjuster);
  }

  private ViewPort(Theme theme, Consumer<Node> nodeAdjuster) {
    super(StandardTransitions.fade());

    this.theme = theme;
    this.nodeAdjuster = nodeAdjuster;
  }

  private ViewPort(Theme theme, ParentPresentation parentPresentation, Consumer<Node> nodeAdjuster) {
    this(theme, nodeAdjuster);

    ChangeListener<? super Presentation> listener = (obs, old, current) -> updateChildNode(theme, parentPresentation, current);

    parentPresentation.childPresentation.when(Nodes.showing(this)).addListener(listener);

    updateChildNode(theme, parentPresentation, parentPresentation.childPresentation.get());
  }

  private ViewPort(Theme theme, Presentation presentation, Consumer<Node> nodeAdjuster) {
    this(theme, nodeAdjuster);

    updateChildNode(theme, null, presentation);
  }

  private Node updateChildNode(Theme theme, ParentPresentation parentPresentation, Presentation current) {
    if(current == null) {
      return null;
    }

    Node node = theme.findPlacer(parentPresentation, current).place(parentPresentation, current);

    if(current instanceof ParentPresentation pp) {
      node.addEventHandler(NavigateEvent.NAVIGATION_TO, e -> handleNavigateEvent(e, pp));
    }
    if(current instanceof Navigable n) {
      node.addEventHandler(NavigateEvent.NAVIGATION_BACK, n::navigateBack);
    }

    Presentations.associate(node, current);

    add(node);  // after transition ends, node will be the single visible node

    if(nodeAdjuster != null) {
      nodeAdjuster.accept(node);
    }

    return node;
  }

  private void handleNavigateEvent(NavigateEvent event, ParentPresentation ancestor) {
    if(theme.nestPresentation(ancestor, event.getPresentation())) {
      event.consume();
    }
  }
}

package hs.mediasystem.presentation;

import java.util.function.Consumer;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class ViewPort extends StackPane {
  private final ParentPresentation presentation;
  private final Consumer<Node> nodeAdjuster;

  public ViewPort(Theme theme, ParentPresentation presentation, Consumer<Node> nodeAdjuster) {
    this.presentation = presentation;
    this.nodeAdjuster = nodeAdjuster;

    presentation.childPresentationProperty().addListener((obs, old, current) -> {
      updateChildNode(theme, current);
    });

    updateChildNode(theme, presentation.childPresentationProperty().get());
  }

  protected Node updateChildNode(Theme theme, Presentation current) {
    if(current == null) {
      return null;
    }

    Node node = theme.findPlacer(presentation, current).place(presentation, current);

    getChildren().setAll(node);

    if(nodeAdjuster != null) {
      nodeAdjuster.accept(node);
    }

    return node;
  }
}

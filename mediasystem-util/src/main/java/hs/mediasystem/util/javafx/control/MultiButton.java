package hs.mediasystem.util.javafx.control;

import hs.mediasystem.util.javafx.control.transition.EffectList;
import hs.mediasystem.util.javafx.control.transition.TransitionPane;
import hs.mediasystem.util.javafx.control.transition.effects.Fade;
import hs.mediasystem.util.javafx.control.transition.effects.Slide;
import hs.mediasystem.util.javafx.control.transition.effects.Slide.Direction;
import hs.mediasystem.util.javafx.control.transition.multi.Custom;

import java.util.List;

import javafx.animation.Interpolator;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class MultiButton extends StackPane {
  private final TransitionPane transitionPane = new TransitionPane(new Custom(
    new EffectList(Duration.millis(250), List.of(new Slide(Interpolator.EASE_BOTH, Direction.DOWN), new Fade())),
    new EffectList(Duration.millis(250), List.of(new Slide(Interpolator.EASE_BOTH, Direction.UP), new Fade()))
  ));
  private final List<Node> nodes;
  private final Label overlay1 = Labels.create("overlay-up-arrow");
  private final Label overlay2 = Labels.create("overlay-down-arrow");

  private int activeIndex;

  public MultiButton(List<Button> children) {
    this.nodes = List.copyOf(children);

    for(Node node : nodes) {
      getChildren().add(node);

      node.setVisible(false);
      node.sceneProperty().addListener((obs, old, current) -> {
        if(current == null && !node.isManaged()) {
          node.setVisible(false);
          node.setManaged(true);

          getChildren().add(node);
        }
      });
    }

    this.getChildren().add(transitionPane);
    this.getChildren().add(overlay1);
    this.getChildren().add(overlay2);

    transitionPane.setClipContent(false);

    if(children.size() > 1) {
      overlay1.setText("▲");
      overlay2.setText("▼");
    }

    nodes.get(0).setVisible(true);

    transitionPane.add(nodes.get(0));
    getChildren().remove(nodes.get(0));

    transitionPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCode().isNavigationKey()) {
        int newIndex = activeIndex;

        if(KeyCode.UP == e.getCode() && activeIndex > 0) {
          newIndex = activeIndex - 1;
        }
        else if(KeyCode.DOWN == e.getCode() && activeIndex < nodes.size() - 1) {
          newIndex = activeIndex + 1;
        }

        if(newIndex != activeIndex) {
          Node child = nodes.get(newIndex);

          getChildren().remove(child);

          child.setVisible(true);

          transitionPane.add(newIndex < activeIndex, child);
          activeIndex = newIndex;
          e.consume();
        }
      }
    });
  }
}

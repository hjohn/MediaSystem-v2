package hs.mediasystem.util.javafx.control.transition.multi;

import hs.mediasystem.util.javafx.control.transition.MultiNodeTransition;

import java.util.List;
import java.util.stream.IntStream;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.util.Duration;

public class Scroll implements MultiNodeTransition {
  private static final String PREFIX = "TransitionPane-";
  private static final String INDEX_KEY = PREFIX + "index";
  private static final String LATEST_KEY = PREFIX + "latest";
  private static final String TRANSITION_KEY = PREFIX + "transition";

  @Override
  public void restart(List<? extends Node> children, Node targetNode, boolean invert) {
    if(targetNode != null) {
      targetNode.getProperties().remove(INDEX_KEY);  // Remove key from most recently added node, in case it was re-added while animated.
    }

    Duration duration = Duration.millis(500);
    Node latest = children.stream().filter(c -> c.getProperties().containsKey(LATEST_KEY)).findFirst().orElse(null);
    int latestIndex = latest == null ? 0 : (int)latest.getProperties().get(INDEX_KEY);

    // Only do anything if there is a new unmarked node:
    IntStream.range(0, children.size())
      .filter(i -> !children.get(i).getProperties().containsKey(INDEX_KEY))
      .findFirst()
      .ifPresent(newNodeIndex -> {
        if(latest != null) {
          latest.getProperties().remove(LATEST_KEY);
        }

        int newLatestIndex = latestIndex + (newNodeIndex == 0 ? -1 : 1);

        children.get(newNodeIndex).getProperties().put(INDEX_KEY, newLatestIndex);
        children.get(newNodeIndex).getProperties().put(LATEST_KEY, true);

        double width = 0;
        double minX = 0;
        int smallestIndex = Integer.MAX_VALUE;

        for(Node child : children) {
          width = Math.max(width, child.getLayoutBounds().getWidth());
          minX = Math.min(minX, child.getTranslateX());

          ObservableMap<Object, Object> properties = child.getProperties();

          if(properties.get(LATEST_KEY) == null) {
            smallestIndex = Math.min(smallestIndex, (int)properties.get(INDEX_KEY));
          }

          // Kill all animations
          properties.computeIfPresent(TRANSITION_KEY, (k, v) -> {
            ((Transition)v).stop();
            return null;
          });
        }

        for(int i = 0; i < children.size(); i++) {
          Node child = children.get(i);
          TranslateTransition tt = new TranslateTransition(duration);

          tt.setNode(child);
          tt.setInterpolator(Interpolator.LINEAR);

          int index = (int)child.getProperties().get(INDEX_KEY);
          int indexDiff = index - newLatestIndex;

          if(indexDiff != 0) { // slide out
            tt.setToX(width * indexDiff);
            tt.play();
            tt.setOnFinished(new EventHandler<ActionEvent>() {  // WORKAROUND: As Lambda, this gives a NoSuchMethodError sometimes
              @Override
              public void handle(ActionEvent e) {
                child.setManaged(false);
              }
            });
          }
          else {  // slide in
            if(child.getProperties().get(LATEST_KEY) == null) {  // not the latest, but at same index, so duplicate

              /*
               * Special case here.  If there is a single cell (1) and one is added to the left (0),
               * then both cells are visible for a time.  If a cell is added to the right again, which
               * would be at position 1 (same as the original cell), before the animation finishes,
               * then we'd have two cells at position 1 overlapping.
               *
               * To prevent this, the older overlapping cell is removed below:
               */

              Platform.runLater(() -> child.setManaged(false));
            }
            else {
              tt.setFromX((newLatestIndex - smallestIndex) * width + minX);
              tt.setToX(0);
              tt.play();
            }
          }

          child.getProperties().put(TRANSITION_KEY, tt);
        }
      });
  }
}
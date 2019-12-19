package hs.mediasystem.util.javafx.control.transitionpane;

import java.util.stream.IntStream;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A pane which enables smooth transitions between its child nodes.<p>
 *
 * Although this pane can temporarily contain multiple children, after
 * all transitions have completed only the focused child will remain.
 * The newest child also immediately attains the focus, so even if a
 * transition takes some time to complete, all events will go to the
 * new child.<p>
 *
 * It is ideally suited for smoothly replacing content when a navigation
 * action is triggered.  The transition itself is only cosmetic and
 * will not interfere with input.
 */
public class TransitionPane extends StackPane {  // Maybe use Region, its getChildren method is protected.
  private static final String PREFIX = "TransitionPane-";
  private static final String INDEX_KEY = PREFIX + "index";
  private static final String LATEST_KEY = PREFIX + "latest";
  private static final String TRANSITION_KEY = PREFIX + "transition";

  public TransitionPane(NodeTransition transition, Node initial) {
    if(initial != null) {
      initial.getProperties().put(INDEX_KEY, 0);
      initial.getProperties().put(LATEST_KEY, true);

      children().add(initial);
    }

    children().addListener((Observable o) -> transition.startTransition(TransitionPane.this, children()));

    Rectangle clip = new Rectangle();

    clip.widthProperty().bind(widthProperty());
    clip.heightProperty().bind(heightProperty());

    setClip(clip);
  }

//  @Override // TODO can't do this, JavaFX don't like it... subclass Region I guess
//  public ObservableList<Node> getChildren() {
//    throw new IllegalStateException("Donot access children directly of TransitionPane!");
//  }

  private ObservableList<Node> children() {
    return super.getChildren();
  }

  public void add(int index, Pane child) {
    children().add(index == -1 ? children().size() : index, child);

    focus(child);
  }

  public void add(Pane child) {
    children().add(child);
    focus(child);
  }

  private boolean focus(Pane root) {
    for(Node node : root.getChildrenUnmodifiable()) {
      if(node instanceof Pane) {
        if(focus((Pane)node)) {
          return true;
        }
      }
      else if(node.isFocusTraversable() && !node.isDisabled()) {
        node.requestFocus();
        return true;
      }
    }

    if(root.isFocusTraversable() && !root.isDisabled()) {
      root.requestFocus();
      return true;
    }

    return false;
  }

  public interface NodeTransition {
    void startTransition(Pane pane, ObservableList<? extends Node> children);
  }

  public static class FadeIn implements NodeTransition {
    @Override
    public void startTransition(Pane pane, ObservableList<? extends Node> children) {
      int size = children.size();

      for(int i = size - 1; i >= 0; i--) {
        Node node = children.get(i);
        Timeline timeline = (Timeline)node.getProperties().get("entity-layout.fade-in-timeline");

        if(timeline == null) {
          timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 0)),
            new KeyFrame(Duration.seconds(0.01), new KeyValue(node.opacityProperty(), 0)),
            new KeyFrame(Duration.seconds(0.5), new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_OUT))
          );

          timeline.play();
        }

        if(i < size - 1 && timeline.getKeyFrames().size() != 2) {
          timeline.stop();

          timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), node.getOpacity())),
            new KeyFrame(Duration.seconds(0.5), e -> {
              children.remove(node);
            }, new KeyValue(node.opacityProperty(), 0, Interpolator.EASE_IN))
          );

          timeline.play();
        }

        node.getProperties().put("entity-layout.fade-in-timeline", timeline);
      }
    }
  }

  public static class Scroll implements NodeTransition {
    @Override
    public void startTransition(Pane pane, ObservableList<? extends Node> children) {
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
                  children.remove(child);
                }
              });
            }
            else {  // slide in
              if(child.getProperties().get(LATEST_KEY) == null) {  // not the latest, but at same index, so duplicate
                Platform.runLater(() -> children.remove(child));
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

  public static class SlideIn implements NodeTransition {
    @Override
    public void startTransition(Pane pane, ObservableList<? extends Node> children) {
      int size = children.size();

      for(int i = size - 1; i >= 0; i--) {
        Node node = children.get(i);
        Transition transition = (Transition)node.getProperties().get("entity-layout.slide-right-transition");

        if(transition == null) {
          node.setVisible(false);
          node.setManaged(false);
//          node.setTranslateX(10000); // transition doesn't play immediately, so make sure the node isn't visible immediately by moving it far away

          transition = new SlideInTransition(pane, node, Duration.millis(2000), Duration.millis(500));
          transition.play();
        }

        if(i < size - 1 && transition instanceof SlideInTransition) {
          transition.stop();

          if(!node.isManaged()) {  // If panel not even visible yet, remove it immediately
            Platform.runLater(() -> children.remove(node));
          }
          else {
            transition = new SlideOutTransition(pane, node);
            transition.setOnFinished(e -> children.remove(node));
            transition.play();
          }
        }

        node.getProperties().put("entity-layout.slide-right-transition", transition);
      }
    }
  }
}

package hs.mediasystem.util.javafx.control.transition;

import hs.mediasystem.util.javafx.Nodes;
import hs.mediasystem.util.javafx.control.transition.effects.Fade;
import hs.mediasystem.util.javafx.control.transition.multi.Custom;

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A pane which enables smooth transitions between its child nodes.<p>
 *
 * Although this pane can temporarily contain multiple children, after
 * all transitions have completed only the last child added will remain.
 * The newest child also immediately attains the focus (if one of the
 * current children had focus), so even if a transition takes some time
 * to complete, all events will go to the new child.<p>
 *
 * It is ideally suited for smoothly replacing content when a navigation
 * action is triggered.  The transition itself is only cosmetic and
 * will not interfere with input.
 */
public class TransitionPane extends Region {
  private final MultiNodeTransition transition;

  public TransitionPane(MultiNodeTransition transition, Node initial) {
    this.transition = transition;

    setClipContent(true);

    if(initial != null) {
      add(initial);
    }
  }

  public TransitionPane(MultiNodeTransition transition) {
    this(transition, null);
  }

  public TransitionPane() {
    this(new Custom(new EffectList(Duration.millis(500), List.of(new Fade()))));
  }

  public void setClipContent(boolean clipContent) {
    if(clipContent) {
      Rectangle clip = new Rectangle();

      clip.widthProperty().bind(widthProperty());
      clip.heightProperty().bind(heightProperty());

      setClip(clip);
    }
    else {
      setClip(null);
    }
  }

  private void add(int index, Node child) {
    boolean hadFocus = Nodes.findFocusedNodeInTree(this).isPresent();

    if(!getChildren().contains(child)) {
      child.setManaged(true);
      child.managedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> obs, Boolean old, Boolean managed) {
          if(!managed) {
            child.managedProperty().removeListener(this);
            getChildren().remove(child);
          }
        }
      });

      getChildren().add(index, child);
    }

    transition.restart(getManagedChildren(), child, index == 0 && getChildren().size() > 1);

    if(hadFocus) {
      Nodes.focus(child);
    }
  }

  public void addAtStart(Node child) {
    add(0, child);
  }

  public void addAtEnd(Node child) {
    add(getChildren().size(), child);
  }

  public void add(Node child) {
    addAtEnd(child);
  }

  public void add(boolean start, Node child) {
    add(start ? 0 : getChildren().size(), child);
  }

  public void clear() {
    transition.restart(getManagedChildren(), null, false);
  }

  @Override
  protected void layoutChildren() {
    List<Node> managed = getManagedChildren();
    Insets insets = getInsets();
    double top = insets.getTop();
    double left = insets.getLeft();
    double w = getWidth() - left - insets.getRight();
    double h = getHeight() - top - insets.getBottom();

    for(Node child : managed) {
      Pos childAlignment = StackPane.getAlignment(child);

      layoutInArea(child, left, top, w, h, 0, StackPane.getMargin(child), childAlignment != null ? childAlignment.getHpos() : HPos.CENTER, childAlignment != null ? childAlignment.getVpos() : VPos.CENTER);
    }
  }
}

package hs.mediasystem.util.javafx.ui.transition;

import hs.mediasystem.util.javafx.base.Nodes;
import hs.mediasystem.util.javafx.ui.transition.domain.EffectList;
import hs.mediasystem.util.javafx.ui.transition.domain.MultiNodeTransition;
import hs.mediasystem.util.javafx.ui.transition.effects.Fade;
import hs.mediasystem.util.javafx.ui.transition.multi.Custom;

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
    addAtStart(child);  // adds at start to avoid a focus issue

    /*
     * Focus Issue Bug:
     *
     * If the node is at added at the end, the TransitionPane will still give it the focus
     * as expected. However, if during the animation the added node modifies its structure
     * removing the node that has focus and re-adding a new node (that should have focus)
     * special code in Scene kicks in -- this code detects that a removed node has the focus
     * and will adjust the focus to a node that is actually part of the scene. The
     * algorithm employed for this will find the TransitionPane, then traverse its children
     * in order and the child with the lowest index will receive focus first. This results
     * in the child that is being removed (after the transition completes) receiving focus
     * again. Adding the new child at the start avoids this problem.
     *
     * Some transitions however determine the animation direction from the position the
     * child is in the children list; these still suffer from this problem as for these
     * transition the child is still added at the end sometimes. The Scroll transition would
     * need to be rewritten to determine the direction differently (perhaps with a marker
     * on the child).
     *
     * Other solutions that were considered:
     *
     * - Disable the children that are about to be removed
     *    - The look of the child might change if disabled controls look differently
     *    - The same child may be re-added, but it was permanently modified to be disabled
     * - Disallow focus traversal for children that are about to removed
     *    - The same child may be re-added, but it was permanently modified to not allow focus traversal
     * - Hook into TraversalEngine; this is a private API but is sometimes used to solve similar problems
     * - Fix the problem in Scene; perhaps the algorithm should not select a new node from
     *   the root node, but from the closest parent that is still part of the scene; this information
     *   may be unrecoverable though
     */
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

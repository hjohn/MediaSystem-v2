package hs.mediasystem.util.javafx.ui.transition.domain;

import java.util.List;

import javafx.scene.Node;

public interface MultiNodeTransition {

  /**
   * Starts or restarts (if a new node was added) the transition.  This method is
   * guaranteed to be called exactly once for each node added.<p>
   *
   * The target node indicates which node should be displayed when all transitions
   * complete, or <code>null</code> if pane should be cleared.  If not <code>null</code>
   * it is usually the most recently added node, but it may also be an existing node
   * (which is  still part of the container) that was re-added before the transitions
   * completed.
   *
   * @param nodes the nodes to apply the transition over
   * @param targetNode the node that should be displayed once the transition completes, can be <code>null</code> if pane should be cleared
   * @param invert whether the animation direction should be inverted (only applies to animations that have a sense of direction)
   */
  void restart(List<? extends Node> nodes, Node targetNode, boolean invert);
}

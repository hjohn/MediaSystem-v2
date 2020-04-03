package hs.mediasystem.util.javafx.control.transition;

import java.util.List;

import javafx.scene.Node;

public interface MultiNodeTransition {

  /**
   * Starts or restarts (if a new node was added) the transition.
   *
   * @param nodes the nodes to apply the transition over
   * @param invert whether the animation direction should be inverted (only applies to animations that have a sense of direction)
   */
  void restart(List<? extends Node> nodes, boolean invert);
}

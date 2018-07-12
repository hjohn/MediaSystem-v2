package hs.mediasystem.presentation;

import javafx.scene.Node;

public interface Placer<P extends ParentPresentation, C extends Presentation> {
  Node place(P parentPresentation, C presentation);
}

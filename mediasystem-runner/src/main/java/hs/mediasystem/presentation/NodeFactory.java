package hs.mediasystem.presentation;

import javafx.scene.Node;

public interface NodeFactory<P> {
  Node create(P presentation);
}

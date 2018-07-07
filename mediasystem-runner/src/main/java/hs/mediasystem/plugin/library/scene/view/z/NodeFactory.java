package hs.mediasystem.plugin.library.scene.view.z;

import javafx.scene.Node;

public interface NodeFactory<P> {
  Node create(P presentation);
}

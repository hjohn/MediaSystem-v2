package hs.mediasystem.runner;

import javafx.scene.Node;

public interface ScenePlugin<L, P> extends hs.mediasystem.runner.PluginBase {
  Class<L> getLocationClass();
  P createPresentation();
  Node createNode(P presentation);
}
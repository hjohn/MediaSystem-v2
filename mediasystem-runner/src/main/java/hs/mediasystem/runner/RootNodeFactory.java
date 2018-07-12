package hs.mediasystem.runner;

import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.ViewPort;
import hs.mediasystem.presentation.ViewPortFactory;

import javafx.scene.Node;
import javafx.scene.paint.Color;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RootNodeFactory implements NodeFactory<RootPresentation> {
  @Inject private SceneManager sceneManager;
  @Inject private ViewPortFactory viewPortFactory;

  @Override
  public Node create(RootPresentation presentation) {
    ViewPort viewPort = viewPortFactory.create(presentation, n -> {

      /*
       * Special functionality for a background node:
       */

      Object backgroundNode = n.getProperties().get("background");

      if(backgroundNode != null) {
        sceneManager.setPlayerRoot(backgroundNode);
        sceneManager.fillProperty().set(Color.TRANSPARENT);
      }
      else {
        sceneManager.disposePlayerRoot();
        sceneManager.fillProperty().set(Color.BLACK);
      }
    });

    return viewPort;
  }
}

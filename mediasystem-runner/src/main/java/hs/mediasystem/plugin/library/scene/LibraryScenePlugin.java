package hs.mediasystem.plugin.library.scene;

import hs.mediasystem.runner.ScenePlugin;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LibraryScenePlugin implements ScenePlugin<LibraryLocation, EntityPresentation> {
  @Inject private EntityLayout layout;

  @Override
  public Class<LibraryLocation> getLocationClass() {
    return LibraryLocation.class;
  }

  @Override
  public EntityPresentation createPresentation() {
    return layout.createPresentation();
  }

  @Override
  public Node createNode(EntityPresentation presentation) {
    return layout.createView(presentation);
  }
}

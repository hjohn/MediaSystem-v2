package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.runner.ScenePlugin;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackScenePlugin implements ScenePlugin<PlaybackLocation, PlaybackOverlayPresentation> {
  @Inject private PlaybackLayout layout;

  @Override
  public Class<PlaybackLocation> getLocationClass() {
    return PlaybackLocation.class;
  }

  @Override
  public PlaybackOverlayPresentation createPresentation() {
    return layout.createPresentation();
  }

  @Override
  public Node createNode(PlaybackOverlayPresentation presentation) {
    return layout.createView(presentation);
  }
}

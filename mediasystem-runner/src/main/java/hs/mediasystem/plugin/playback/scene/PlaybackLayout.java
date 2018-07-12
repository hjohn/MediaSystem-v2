package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.presentation.NodeFactory;

import javafx.application.Platform;
import javafx.scene.Node;

import javax.inject.Singleton;

@Singleton
public class PlaybackLayout implements NodeFactory<PlaybackOverlayPresentation> {

  @Override
  public Node create(PlaybackOverlayPresentation presentation) {
    PlaybackOverlayPane view = new PlaybackOverlayPane(presentation);

    view.getProperties().put("background", presentation.playerPresentation.get().getDisplayComponent());

    Platform.runLater(() -> presentation.playerPresentation.get().play(presentation.uri.get().toString(), 0));

    return view;
  }
}

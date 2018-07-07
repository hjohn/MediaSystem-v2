package hs.mediasystem.plugin.playback.scene;

import javafx.application.Platform;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackLayout {
  @Inject private PlayerLayout playerLayout;

  public PlaybackOverlayPresentation createPresentation() {
    return new PlaybackOverlayPresentation(playerLayout.createPresentation());
  }

  public Node createView(PlaybackOverlayPresentation presentation) {
    PlaybackOverlayPane view = new PlaybackOverlayPane();

    view.location.bind(presentation.location);
    view.player.bind(presentation.playerPresentation);
    view.overlayVisible.bind(presentation.overlayVisible);

    view.getProperties().put("background", presentation.playerPresentation.get().getDisplayComponent());

    view.location.addListener((obs, old, current) -> {
      Platform.runLater(() -> presentation.playerPresentation.get().play(current.getUri().toString(), 0));
    });

    return view;
  }
}

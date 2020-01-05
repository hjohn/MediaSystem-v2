package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.PlayerEvent;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.ImageHandleFactory;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackLayout implements NodeFactory<PlaybackOverlayPresentation> {
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public Node create(PlaybackOverlayPresentation presentation) {
    PlaybackOverlayPane view = new PlaybackOverlayPane(presentation, imageHandleFactory);

    view.getProperties().put("background", presentation.playerPresentation.get().getDisplayComponent());

    presentation.playerPresentation.get().onPlayerEvent().set(new EventHandler<PlayerEvent>() {
      @Override
      public void handle(PlayerEvent event) {
        if(event.getType() == PlayerEvent.Type.FINISHED) {
          Platform.runLater(() -> Event.fireEvent(view, NavigateEvent.back()));
          event.consume();
        }
      }
    });

    Platform.runLater(() -> presentation.playerPresentation.get().play(presentation.uri.toString(), presentation.startPosition.toMillis()));

    return view;
  }
}

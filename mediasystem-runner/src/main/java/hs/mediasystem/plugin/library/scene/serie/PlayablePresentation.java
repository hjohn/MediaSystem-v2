package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.javafx.Action;
import hs.mediasystem.util.javafx.SimpleAction;

import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Provider;

import org.reactfx.value.Val;

public class PlayablePresentation extends AbstractPresentation {
  @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  @Inject private StreamStateService streamStateService;

  public final ObjectProperty<MediaItem<?>> playable = new SimpleObjectProperty<>();

  private final Val<MediaItem<?>> playItem = Val.wrap(playable).filter(p -> !(p.getData() instanceof Serie));

  public final Val<Integer> resumePosition = Val.wrap(playItem)
    .filter(Objects::nonNull)
    .map(playItem -> streamStateService.getResumePosition(playItem.getStreams().iterator().next().getStreamPrint()))
    .orElseConst(-1);

  public final Action play = new SimpleAction("Play", playItem.map(Objects::nonNull).orElseConst(false), this::play);
  public final Action resume = new SimpleAction("Resume", resumePosition.map(rp -> rp > 0), this::resume);

  private void play(Event event) {
    playItem.ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(p, p.getStreams().iterator().next().getUri(), 0))));
  }

  private void resume(Event event) {
    playItem.ifPresent(p -> Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(p, p.getStreams().iterator().next().getUri(), resumePosition.getValue() * 1000))));
  }
}

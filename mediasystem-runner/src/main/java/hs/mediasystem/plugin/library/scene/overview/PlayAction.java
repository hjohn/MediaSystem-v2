package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.javafx.action.Action;

import java.util.Objects;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.reactfx.value.Val;

public class PlayAction implements Action {

  @Singleton
  public static class Factory {
    @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;

    public PlayAction create(ObservableValue<Work> mediaItem) {
      return new PlayAction(playbackOverlayPresentationProvider, mediaItem);
    }
  }

  private final Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  private final Val<Work> playableMediaItem;
  private final Val<Boolean> enabled;

  private PlayAction(Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider, ObservableValue<Work> mediaItem) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;
    this.playableMediaItem = Val.wrap(mediaItem)
      .filter(Objects::nonNull)
      .filter(r -> !r.getStreams().isEmpty());
    this.enabled = Val.map(playableMediaItem, Objects::nonNull).orElseConst(false);
  }

  @Override
  public StringExpression titleProperty() {
    return new SimpleStringProperty("Play");
  }

  @Override
  public Val<Boolean> enabledProperty() {
    return enabled;
  }

  @Override
  public void trigger(Event event) {
    playableMediaItem.ifPresent(r -> {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(r, r.getPrimaryStream().orElseThrow().getAttributes().getUri(), 0)));
      event.consume();
    });
  }
}

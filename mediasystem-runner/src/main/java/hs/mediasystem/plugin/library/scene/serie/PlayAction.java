package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.plugin.library.scene.MediaItem;
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

    public PlayAction create(ObservableValue<MediaItem<?>> mediaItem) {
      return new PlayAction(playbackOverlayPresentationProvider, mediaItem);
    }
  }

  private final Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  private final Val<MediaItem<?>> playableMediaItem;
  private final Val<Boolean> enabled;

  private PlayAction(Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider, ObservableValue<MediaItem<?>> mediaItem) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;
    this.playableMediaItem = Val.wrap(mediaItem)
      .filter(Objects::nonNull)
      .filter(mi -> mi.getData() instanceof Movie || mi.getData() instanceof Episode)
      .filter(mi -> !mi.getStreams().isEmpty());
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
    playableMediaItem.ifPresent(mi -> {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(mi, mi.getStreams().iterator().next().getUri(), 0)));
      event.consume();
    });
  }
}

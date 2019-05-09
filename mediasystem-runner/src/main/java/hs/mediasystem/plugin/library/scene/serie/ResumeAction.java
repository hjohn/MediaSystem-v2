package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.StreamStateService;
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

public class ResumeAction implements Action {

  @Singleton
  public static class Factory {
    @Inject private StreamStateService streamStateService;
    @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;

    public ResumeAction create(ObservableValue<MediaItem<?>> mediaItem) {
      return new ResumeAction(streamStateService, playbackOverlayPresentationProvider, mediaItem);
    }
  }

  public final Val<Integer> resumePosition;

  private final Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  private final Val<MediaItem<?>> resumableMediaItem;
  private final Val<Boolean> enabled;

  private ResumeAction(StreamStateService streamStateService, Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider, ObservableValue<MediaItem<?>> mediaItem) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;

    Val<MediaItem<?>> playableMediaItem = Val.filter(mediaItem, Objects::nonNull)
      .filter(mi -> mi.getData() instanceof Movie || mi.getData() instanceof Episode)
      .filter(mi -> !mi.getStreams().isEmpty());

    this.resumePosition = playableMediaItem
      .flatMap(playItem -> streamStateService.resumePositionProperty(playItem.getStreams().iterator().next().getStreamPrint()))
      .orElseConst(-1);

    this.resumableMediaItem = Val.combine(resumePosition, playableMediaItem, (rp, mi) -> rp > 0 && mi != null ? mi : null);
    this.enabled = Val.map(resumableMediaItem, Objects::nonNull).orElseConst(false);
  }

  @Override
  public StringExpression titleProperty() {
    return new SimpleStringProperty("Resume");
  }

  @Override
  public Val<Boolean> enabledProperty() {
    return enabled;
  }

  @Override
  public void trigger(Event event) {
    resumableMediaItem.ifPresent(mi -> {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(mi, mi.getStreams().iterator().next().getUri(), resumePosition.getValue() * 1000L)));
      event.consume();
    });
  }
}


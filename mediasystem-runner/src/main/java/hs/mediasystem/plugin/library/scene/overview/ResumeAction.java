package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.db.StreamStateService;
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

public class ResumeAction implements Action {

  @Singleton
  public static class Factory {
    @Inject private StreamStateService streamStateService;
    @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;

    public ResumeAction create(ObservableValue<Work> work) {
      return new ResumeAction(streamStateService, playbackOverlayPresentationProvider, work);
    }
  }

  public final Val<Integer> resumePosition;

  private final Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  private final Val<Work> resumableWork;
  private final Val<Boolean> enabled;

  private ResumeAction(StreamStateService streamStateService, Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider, ObservableValue<Work> work) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;

    Val<Work> playableWork = Val.filter(work, Objects::nonNull)
      .filter(r -> !r.getStreams().isEmpty());

    this.resumePosition = playableWork
      .flatMap(playItem -> streamStateService.resumePositionProperty(playItem.getPrimaryStream().orElseThrow().getId()))
      .orElseConst(-1);

    this.resumableWork = Val.combine(resumePosition, playableWork, (rp, r) -> rp > 0 && r != null ? r : null);
    this.enabled = Val.map(resumableWork, Objects::nonNull).orElseConst(false);
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
    resumableWork.ifPresent(r -> {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(r, r.getPrimaryStream().orElseThrow().getAttributes().getUri(), resumePosition.getValue() * 1000L)));
      event.consume();
    });
  }
}


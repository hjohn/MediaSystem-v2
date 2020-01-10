package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.ui.api.domain.State;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.javafx.action.Action;

import java.time.Duration;
import java.util.Objects;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Val;

public class ResumeAction implements Action {

  @Singleton
  public static class Factory {
    @Inject private PlaybackOverlayPresentation.Factory factory;

    public ResumeAction create(ObservableValue<Work> work) {
      return new ResumeAction(factory, work);
    }
  }

  public final Val<Duration> resumePosition;

  private final PlaybackOverlayPresentation.Factory factory;
  private final Val<Work> resumableWork;
  private final Val<Boolean> enabled;

  private ResumeAction(PlaybackOverlayPresentation.Factory factory, ObservableValue<Work> work) {
    this.factory = factory;

    Val<Work> playableWork = Val.filter(work, Objects::nonNull)
      .filter(w -> !w.getStreams().isEmpty());

    this.resumePosition = playableWork.map(Work::getState).flatMap(State::getResumePosition);
    this.resumableWork = Val.combine(resumePosition, playableWork, (rp, w) -> rp.toSeconds() > 0 && w != null ? w : null);
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
    resumableWork.ifPresent(w -> {
      Event.fireEvent(event.getTarget(), NavigateEvent.to(factory.create(w, w.getPrimaryStream().orElseThrow().getAttributes().getUri(), resumePosition.getValue())));
      event.consume();
    });
  }
}


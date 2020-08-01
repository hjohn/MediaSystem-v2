package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.PresentationLoader;
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

public class PlayAction implements Action {

  @Singleton
  public static class Factory {
    @Inject private PlaybackOverlayPresentation.TaskFactory factory;

    public PlayAction create(ObservableValue<Work> work) {
      return new PlayAction(factory, work);
    }
  }

  private final PlaybackOverlayPresentation.TaskFactory factory;
  private final Val<Work> playableMediaItem;
  private final Val<Boolean> enabled;

  private PlayAction(PlaybackOverlayPresentation.TaskFactory factory, ObservableValue<Work> work) {
    this.factory = factory;
    this.playableMediaItem = Val.wrap(work)
      .filter(Objects::nonNull)
      .filter(w -> !w.getStreams().isEmpty());
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
    playableMediaItem.ifPresent(w -> {
      PresentationLoader.navigate(event, factory.create(w, w.getPrimaryStream().orElseThrow().getAttributes().getUri(), Duration.ZERO));
    });
  }
}

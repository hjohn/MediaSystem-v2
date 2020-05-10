package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.ui.api.player.PlayerPresentation;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.value.Var;

public class PlaybackOverlayPresentation implements Navigable, Presentation {
  private static final Logger LOGGER = Logger.getLogger(PlaybackOverlayPresentation.class.getName());

  public final Work work;
  public final ObjectProperty<PlayerPresentation> playerPresentation = new SimpleObjectProperty<>();
  public final BooleanProperty overlayVisible = new SimpleBooleanProperty(true);
  public final URI uri;
  public final Duration startPosition;

  @Singleton
  public static class Factory {
    @Inject private PlayerSetting playerSetting;

    public PlaybackOverlayPresentation create(Work work, URI uri, Duration startPosition) {
      return new PlaybackOverlayPresentation(playerSetting, work, uri, startPosition);
    }
  }

  private PlaybackOverlayPresentation(PlayerSetting playerSetting, Work work, URI uri, Duration startPosition) {
    this.work = work;
    this.uri = uri;
    this.startPosition = startPosition;
    this.playerPresentation.set(playerSetting.get());

    this.playerPresentation.get().positionProperty().addListener(new ChangeListener<Number>() {
      private long totalTimeViewed;
      private long timeViewedSinceLastSkip;

      @Override
      public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number value) {
        long old = oldValue.longValue();
        long current = value.longValue();

        if(old < current) {
          long diff = current - old;

          if(diff > 0 && diff < 4000) {
            totalTimeViewed += diff;
            timeViewedSinceLastSkip += diff;

            updatePositionAndViewed();
          }

          if(Math.abs(diff) >= 4000) {
            timeViewedSinceLastSkip = 0;
          }
        }
      }

      private void updatePositionAndViewed() {
        PlayerPresentation player = PlaybackOverlayPresentation.this.playerPresentation.get();
        long length = player.lengthProperty().getValue();

        if(length > 0) {
          long timeViewed = totalTimeViewed + startPosition.toMillis();
          Var<Boolean> consumed = work.getState().isConsumed();

          if(timeViewed >= length * 9 / 10 && !consumed.getValue()) {   // 90% viewed and not viewed yet?
            LOGGER.info("Marking as viewed: " + work);

            consumed.setValue(true);
          }

          if(timeViewedSinceLastSkip > 30 * 1000) {
            int resumePosition = 0;
            long position = player.positionProperty().getValue();

            if(position > 30 * 1000 && position < length * 9 / 10) {
              resumePosition = (int)(position / 1000) - 10;
            }

            Var<Duration> storedResumePosition = work.getState().getResumePosition();

            if(Math.abs(storedResumePosition.getValue().toSeconds() - resumePosition) > 10) {
              Instant now = Instant.now();

              storedResumePosition.setValue(Duration.ofSeconds(resumePosition));
              work.getState().getLastConsumptionTime().setValue(now);
            }
          }
        }
      }
    });
  }

  @Override
  public void navigateBack(Event e) {
    playerPresentation.get().dispose();
  }
}

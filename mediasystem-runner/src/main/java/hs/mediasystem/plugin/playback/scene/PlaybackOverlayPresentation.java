package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.StringURI;

import java.time.Instant;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class PlaybackOverlayPresentation implements Navigable, Presentation {
  private static final Logger LOGGER = Logger.getLogger(PlaybackOverlayPresentation.class.getName());

  public final ObjectProperty<MediaItem<?>> parentMediaItem = new SimpleObjectProperty<>();
  public final ObjectProperty<MediaItem<?>> mediaItem = new SimpleObjectProperty<>();
  public final ObjectProperty<StringURI> uri = new SimpleObjectProperty<>();
  public final LongProperty startPositionMillis = new SimpleLongProperty();

  public final ObjectProperty<PlayerPresentation> playerPresentation = new SimpleObjectProperty<>();

  public final BooleanProperty overlayVisible = new SimpleBooleanProperty(true);

  @Inject private PlayerSetting playerSetting;
  @Inject private StreamStateService streamStateService;

  public PlaybackOverlayPresentation set(MediaItem<?> mediaItem, StringURI uri, long startPositionMillis) {
    this.parentMediaItem.set(mediaItem.getParent());
    this.mediaItem.set(mediaItem);
    this.uri.set(uri);
    this.startPositionMillis.set(startPositionMillis);

    return this;
  }

//  @Inject private Set<SubtitleProvider> subtitleProviders;
//  @Inject private Set<SubtitleCriteriaProvider> subtitleCriteriaProviders;

  @PostConstruct
  private void postConstruct() {
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
        mediaItem.get().getStreams().stream()
          .filter(s -> s.getUri().equals(uri.get()))
          .findFirst()
          .map(BasicStream::getId)
          .ifPresent(this::updatePositionAndViewed);
      }

      private void updatePositionAndViewed(StreamID streamId) {
        PlayerPresentation player = PlaybackOverlayPresentation.this.playerPresentation.get();
        long length = player.lengthProperty().getValue();

        if(length > 0) {
          long timeViewed = totalTimeViewed + startPositionMillis.get();
          boolean watched = streamStateService.isWatched(streamId);

          if(timeViewed >= length * 9 / 10 && !watched) {   // 90% viewed and not viewed yet?
            LOGGER.config("Marking as viewed: " + mediaItem.get());

            streamStateService.setWatched(streamId, true);
          }

          if(timeViewedSinceLastSkip > 30 * 1000) {
            int resumePosition = 0;
            long position = player.positionProperty().getValue();

            if(position > 30 * 1000 && position < length * 9 / 10) {
              resumePosition = (int)(position / 1000) - 10;
            }

            int storedResumePosition = streamStateService.getResumePosition(streamId);

            if(Math.abs(storedResumePosition - resumePosition) > 10) {
              Instant now = Instant.now();

              streamStateService.setResumePosition(streamId, resumePosition);
              streamStateService.setTotalDuration(streamId, (int)(length / 1000));
              streamStateService.setLastWatchedTime(streamId, now);

              if(parentMediaItem.get() != null) {
                streamStateService.setLastWatchedTime(parentMediaItem.get().getStream().getId(), now);
              }
            }
          }
        }
      }
    });
  }

//  @Expose
//  public void chooseSubtitle(Event event) {
//    Dialogs.show(event, new DialogPane<Void>() {{
//      getChildren().add(new SubtitleDownloadPane(media.get(), subtitleProviders, subtitleCriteriaProviders, controller.getSubtitleDownloadService()));
//    }});
//    event.consume();
//  }

  @Override
  public void navigateBack(Event e) {
    System.out.println("PlaybackOverlayPresentation, consumed = " + e.isConsumed());
    playerPresentation.get().stop();
  }
}

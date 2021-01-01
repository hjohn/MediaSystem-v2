package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.ui.api.StreamStateClient;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.ui.api.player.PlayerPresentation;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Singleton;

public class PlaybackOverlayPresentation implements Navigable, Presentation {
  private static final Logger LOGGER = Logger.getLogger(PlaybackOverlayPresentation.class.getName());

  public final Work work;
  public final ObjectProperty<PlayerPresentation> playerPresentation = new SimpleObjectProperty<>();
  public final BooleanProperty overlayVisible = new SimpleBooleanProperty(true);
  public final URI uri;
  public final Duration startPosition;

  @Singleton
  public static class TaskFactory {
    @Inject private PlayerSetting playerSetting;
    @Inject private StreamStateClient streamStateClient;

    public Task<PlaybackOverlayPresentation> create(Work work, URI uri, Duration startPosition) {
      return new Task<>() {
        @Override
        protected PlaybackOverlayPresentation call() {
          updateTitle("Playing Video...");
          updateMessage("Please wait...");

          if(uri.getScheme().equals("file")) {
            updateProgress(1, 10);

            try(FileChannel fc = FileChannel.open(Paths.get(uri), StandardOpenOption.READ)) {
              ByteBuffer buf = ByteBuffer.allocate(16);

              // Read some data from the first 10-20 MB, to make sure stream is "ready" (hard disk spinning up)
              for(int i = 0, j = 0; j < 10; i += 4096 + i * 2, j++) {
                fc.read(buf);
                fc.position(i);

                buf.clear();

                updateProgress(j, 10);
              }
            }
            catch(IOException e) {
              throw new VideoUnavailableException(e, Paths.get(uri));
            }
          }

          PlayerPresentation playerPresentation = playerSetting.getConfigured();

          if(playerPresentation == null) {
            throw new MissingPlayerPresentationException(playerSetting.getConfiguredName(), playerSetting.getAvailablePlayerFactories());
          }

          return new PlaybackOverlayPresentation(streamStateClient, playerPresentation, work, uri, startPosition);
        }
      };
    }
  }

  private PlaybackOverlayPresentation(StreamStateClient streamStateClient, PlayerPresentation playerPresentation, Work work, URI uri, Duration startPosition) {
    this.work = work;
    this.uri = uri;
    this.startPosition = startPosition;
    this.playerPresentation.set(playerPresentation);

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

            updatePositionAndViewed();  // TODO this involves db communication / server communication, may not want to do that on FX thread
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
          ContentID contentId = work.getPrimaryStream().orElseThrow().getId().getContentId();

          BooleanProperty consumed = streamStateClient.watchedProperty(contentId);

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

            ObjectProperty<Integer> storedResumePosition = streamStateClient.resumePositionProperty(contentId);

            if(Math.abs(storedResumePosition.getValue() - resumePosition) > 10) {
              Instant now = Instant.now();

              storedResumePosition.setValue(resumePosition);
              streamStateClient.lastWatchedTimeProperty(contentId).set(now);
            }
          }
        }
      }
    });
  }

  @Override
  public void navigateBack(Event e) {
    PlayerPresentation presentation = playerPresentation.get();

    Dialogs.showProgressDialog(e, false, new Task<>() {
      @Override
      protected Void call() throws Exception {
        updateTitle("Stopping Video...");

        presentation.dispose();  // dispose can take a while with VLC, do it asynchronously so JavaFX does not freeze

        return null;
      }
    });
  }
}

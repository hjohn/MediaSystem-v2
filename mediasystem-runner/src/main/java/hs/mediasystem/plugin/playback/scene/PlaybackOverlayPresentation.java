package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.ui.api.StreamStateClient;
import hs.mediasystem.ui.api.domain.MediaStream;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.ui.api.player.PlayerPresentation;
import hs.mediasystem.util.concurrent.NamedExecutors;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
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
  private static final Executor EXECUTOR = NamedExecutors.newSingleTaskExecutor(PlaybackOverlayPresentation.class.getName());

  public final Work work;
  public final ObjectProperty<PlayerPresentation> playerPresentation = new SimpleObjectProperty<>();
  public final BooleanProperty overlayVisible = new SimpleBooleanProperty(true);
  public final URI uri;
  public final Duration startPosition;

  private final ConsumptionUpdater consumptionUpdater;

  @Singleton
  public static class TaskFactory {
    @Inject private PlayerSetting playerSetting;
    @Inject private StreamStateClient streamStateClient;

    private Task<PlaybackOverlayPresentation> create(Work work, MediaStream stream, URI uri, Duration startPosition) {
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

          return new PlaybackOverlayPresentation(streamStateClient, playerPresentation, work, stream, uri, startPosition);
        }
      };
    }

    public Task<PlaybackOverlayPresentation> create(Work work, MediaStream stream, Duration startPosition) {
      return create(work, stream, stream.uri(), startPosition);
    }

    public Task<PlaybackOverlayPresentation> create(Work work, URI uri, Duration startPosition) {
      return create(work, null, uri, startPosition);
    }
  }

  /**
   * Constructs a new instance.
   *
   * @param streamStateClient a {@link StreamStateClient}, cannot be null
   * @param playerPresentation a {@link PlayerPresentation}, cannot be null
   * @param work a {@link Work} with which the {@link URI} is associated (not necessarily one of its streams), cannot be null
   * @param stream a {@link MediaStream} which is part of the given {@link Work}, can be null in case a stream not part of the work is played
   * @param uri a {@link URI} to play, cannot be null
   * @param startPosition the position to start playback at, cannot be null
   */
  private PlaybackOverlayPresentation(StreamStateClient streamStateClient, PlayerPresentation playerPresentation, Work work, MediaStream stream, URI uri, Duration startPosition) {
    this.work = work;
    this.uri = uri;
    this.startPosition = startPosition;
    this.playerPresentation.set(playerPresentation);

    ContentID contentId = stream == null ? null : stream.contentId();

    this.consumptionUpdater = contentId == null ? null : new ConsumptionUpdater(streamStateClient, work, contentId);

    if(consumptionUpdater != null) {
      this.playerPresentation.get().positionProperty().addListener(new ChangeListener<Number>() {
        private long totalTimeViewed;
        private long timeViewedSinceLastSkip;
        private long lastCallTime = Long.MIN_VALUE;

        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number value) {
          long old = oldValue.longValue();
          long current = value.longValue();

          if(old < current) {
            long diff = current - old;

            if(diff > 0 && diff < 4000) {
              totalTimeViewed += diff;
              timeViewedSinceLastSkip += diff;

              long length = playerPresentation.lengthProperty().get();

              if(length > 0 && lastCallTime < System.currentTimeMillis() - 10 * 1000) {
                lastCallTime = System.currentTimeMillis();

                EXECUTOR.execute(() -> consumptionUpdater.updatePositionAndViewed(
                  playerPresentation.positionProperty().get(),
                  length,
                  totalTimeViewed + startPosition.toMillis(),
                  timeViewedSinceLastSkip
                ));
              }
            }

            if(Math.abs(diff) >= 4000) {
              timeViewedSinceLastSkip = 0;
            }
          }
        }
      });
    }
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

  /**
   * Updates the consumption state of an item currently being consumed.
   *
   * <p>This is a separate class as the update process must happen on a different thread
   * (as it is making potentially slow calls), and having the class separate and static
   * makes it easier to avoid sharing state.
   */
  private static class ConsumptionUpdater {
    private final StreamStateClient streamStateClient;
    private final Work work;
    private final ContentID contentId;

    private Boolean consumed;

    ConsumptionUpdater(StreamStateClient streamStateClient, Work work, ContentID contentId) {
      this.streamStateClient = streamStateClient;
      this.work = work;
      this.contentId = contentId;
    }

    void updatePositionAndViewed(long position, long length, long timeViewed, long timeViewedSinceLastSkip) {
      if(consumed == null) {
        consumed = streamStateClient.isConsumed(contentId);
      }

      if(timeViewed >= length * 9 / 10 && !consumed) {   // 90% viewed and not viewed yet?
        LOGGER.info("Marking as viewed: " + work);

        streamStateClient.setConsumed(contentId, true);
        streamStateClient.setResumePosition(contentId, Duration.ZERO);
      }

      if(timeViewedSinceLastSkip > 30 * 1000) {
        Duration resumePosition = Duration.ZERO;

        if(position > 30 * 1000 && position < length * 9 / 10) {
          resumePosition = Duration.ofSeconds(position / 1000 - 10);
        }

        streamStateClient.setResumePosition(contentId, resumePosition);
        streamStateClient.setLastConsumptionTime(contentId, Instant.now());
      }
    }
  }
}

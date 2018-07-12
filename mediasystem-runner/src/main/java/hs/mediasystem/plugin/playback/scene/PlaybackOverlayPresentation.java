package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.Navigable;

import java.net.URI;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class PlaybackOverlayPresentation implements Navigable, Presentation {
  public final ObjectProperty<MediaItem<?>> mediaItem = new SimpleObjectProperty<>();
  public final ObjectProperty<URI> uri = new SimpleObjectProperty<>();

  @Inject private PlayerSetting playerSetting;

  @Expose(name = "player")
  public final ObjectProperty<PlayerPresentation> playerPresentation = new SimpleObjectProperty<>();

  @Expose
  public final BooleanProperty overlayVisible = new SimpleBooleanProperty(true);

  public PlaybackOverlayPresentation set(MediaItem<?> mediaItem, URI uri) {
    this.mediaItem.set(mediaItem);
    this.uri.set(uri);

    return this;
  }

  // TODO this binding is ugly, but it prevents a permanent reference to Player...
  //private final LongBinding position = Bindings.selectLong(player, "position");

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

            // updatePositionAndViewed();
          }

          if(Math.abs(diff) >= 4000) {
            timeViewedSinceLastSkip = 0;
          }
        }
      }

      /*
      private void updatePositionAndViewed() {
        Player player = PlaybackOverlayPresentation.this.player.get();
        MediaData mediaData = media.get().getMediaItem().mediaData.get();

        if(mediaData != null) {
          long length = player.getLength();

          if(length > 0) {
            // TODO PlaybackLocation could be used to facilite skipping, not just the initial start position.  So skipping etc can be accomplished by location; however careful that for the same media initial resume position must be preserved for proper "viewed" calculation.
            long timeViewed = totalTimeViewed + location.get().getStartMillis();

            if(!mediaData.viewed.get() && timeViewed >= length * 9 / 10) {  // 90% viewed?
              System.out.println("[CONFIG] PlaybackOverlayPresentation - Marking as viewed: " + media.get());

              mediaData.viewed.set(true);
            }

            if(timeViewedSinceLastSkip > 30 * 1000) {
              int resumePosition = 0;
              long position = player.getPosition();

              if(position > 30 * 1000 && position < length * 9 / 10) {
                resumePosition = (int)(position / 1000) - 10;
              }

              if(Math.abs(mediaData.resumePosition.get() - resumePosition) > 10) {
                mediaData.resumePosition.set(resumePosition);
              }
            }
          }
        }
      }
      */
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

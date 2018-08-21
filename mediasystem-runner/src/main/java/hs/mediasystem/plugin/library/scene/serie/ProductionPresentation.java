package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.scan.StreamPrint;
import hs.mediasystem.framework.actions.Expose;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Dialogs;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.javafx.Action;
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.SimpleAction;
import hs.mediasystem.util.javafx.Val;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

public class ProductionPresentation extends AbstractPresentation implements Navigable {

  @Singleton
  public static class Factory {
    @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
    @Inject private VideoDatabase videoDatabase;
    @Inject private StreamStateService streamStateService;
    @Inject private Provider<EpisodesPresentation> episodesPresentationProvider;
    @Inject private PlayAction.Factory playActionFactory;
    @Inject private ResumeAction.Factory resumeActionFactory;

    public ProductionPresentation create(MediaItem<?> mediaItem) {
      return new ProductionPresentation(
        playbackOverlayPresentationProvider,
        videoDatabase,
        streamStateService,
        episodesPresentationProvider,
        playActionFactory,
        resumeActionFactory,
        mediaItem
      );
    }
  }

  public enum State {
    OVERVIEW, LIST, EPISODE
  }

  public enum ButtonState {
    MAIN, PLAY_RESUME, RELATED
  }

  private final Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
  private final VideoDatabase videoDatabase;
  private final StreamStateService streamStateService;

  private final ObjectProperty<State> internalState = new SimpleObjectProperty<>(State.OVERVIEW);
  private final ObjectProperty<ButtonState> internalButtonState = new SimpleObjectProperty<>(ButtonState.MAIN);

  private final ObjectProperty<VideoLink> trailerVideoLink = new SimpleObjectProperty<>();

  public final ReadOnlyObjectProperty<State> state = objectValue(internalState);
  public final ReadOnlyObjectProperty<ButtonState> buttonState = objectValue(internalButtonState);
  public final Val<MediaItem<?>> episodeOrMovieItem;

  public final MediaItem<?> rootItem;

  public final PlayAction play;
  public final ResumeAction resume;
  public final Action playTrailer = new SimpleAction("Trailer", trailerVideoLink.isNotNull(), this::playTrailer);

  public final EpisodesPresentation episodesPresentation;

  private ProductionPresentation(
    Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider,
    VideoDatabase videoDatabase,
    StreamStateService streamStateService,
    Provider<EpisodesPresentation> episodesPresentationProvider,
    PlayAction.Factory playActionFactory,
    ResumeAction.Factory resumeActionFactory,
    MediaItem<?> mediaItem
  ) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;
    this.videoDatabase = videoDatabase;
    this.streamStateService = streamStateService;
    this.rootItem = mediaItem;
    this.episodesPresentation = mediaItem.getData() instanceof Serie ? episodesPresentationProvider.get().set((MediaItem<Serie>)mediaItem) : null;
    this.episodeOrMovieItem = episodesPresentation == null ? Binds.monadic(rootItem) : (Val<MediaItem<?>>)(Val<?>)Binds.monadic(episodesPresentation.episodeItem);
    this.play = playActionFactory.create(episodeOrMovieItem);
    this.resume = resumeActionFactory.create(episodeOrMovieItem);
  }

  public EpisodesPresentation getEpisodesPresentation() {
    return episodesPresentation;
  }

  @Override
  public void navigateBack(Event e) {
    switch(buttonState.get()) {
    case PLAY_RESUME:
    case RELATED:
      internalButtonState.set(ButtonState.MAIN);
      break;
    case MAIN:
      switch(state.get()) {
      case OVERVIEW:
        return;
      case LIST:
        episodesPresentation.episodeItem.unbind();
        internalState.set(State.OVERVIEW);
        break;
      case EPISODE:
        internalState.set(State.LIST);
        break;
      }
    }

    e.consume();
  }

  public void toEpisodeState() {
    if(this.episodesPresentation.episodeItem.get() == null) {
      throw new IllegalStateException("Cannot go to Episode state without an episode set");
    }

    update();

    this.internalState.set(State.EPISODE);
  }

  public void toListState() {
    this.internalState.set(State.LIST);
  }

  public void toPlayResumeButtonState() {
    this.internalButtonState.set(ButtonState.PLAY_RESUME);
  }

  public void toRelatedButtonState() {
    this.internalButtonState.set(ButtonState.RELATED);
  }

  @Expose
  public void toggleWatchedState() {
    if(internalState.get() == State.LIST) {
      MediaItem<?> mediaItem = episodesPresentation.episodeItem.get();

      if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
        StreamPrint streamPrint = mediaItem.getStreams().iterator().next().getStreamPrint();

        // Update state
        boolean watched = streamStateService.isWatched(streamPrint);
        streamStateService.setWatched(streamPrint, !watched);

        // Update MediaItem
        mediaItem.watchedCount.set(!watched ? mediaItem.availableCount.get() : 0);
      }
    }
  }

  enum Option {
    MARK_WATCHED,
    MARK_UNWATCHED
  }

  @Expose
  public void showContextMenu(Event event) {
    if(internalState.get() == State.LIST) {
      // Note: map is sorted according to order of Option enum declaration
      Map<Option, String> map = new EnumMap<>(Option.class);

      Boolean isWatched = isWatched();

      if(isWatched == null) {
        return;
      }

      if(isWatched) {
        map.put(Option.MARK_UNWATCHED, "Mark Not Watched");
      }
      else {
        map.put(Option.MARK_UNWATCHED, "Mark Watched");
      }

      Dialogs.show(event, map).ifPresent(option -> {
        switch(option) {
        case MARK_WATCHED:
        case MARK_UNWATCHED:
          toggleWatchedState();
          break;
        }
      });
    }
  }

  private Boolean isWatched() {
    MediaItem<?> mediaItem = episodesPresentation.episodeItem.get();

    if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
      StreamPrint streamPrint = mediaItem.getStreams().iterator().next().getStreamPrint();

      return streamStateService.isWatched(streamPrint);
    }

    return null;  // Indicates no state possible as there is no stream
  }

  private void playTrailer(Event event) {
    VideoLink videoLink = trailerVideoLink.get();
    Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(getPlayableMediaItem(), new StringURI("https://www.youtube.com/watch?v=" + videoLink.getKey()), 0)));
  }

  public void update() {
    MediaItem<?> mediaItem = getPlayableMediaItem();

    trailerVideoLink.set(null);

    if(mediaItem != null && trailerVideoLink.get() == null) {
      CompletableFuture.supplyAsync(() -> videoDatabase.queryVideoLinks(mediaItem.getProduction().getIdentifier()))
        .thenAccept(videoLinks -> {
          videoLinks.stream().filter(vl -> vl.getType() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> Platform.runLater(() -> {
            if(mediaItem.equals(getPlayableMediaItem())) {
              trailerVideoLink.set(videoLink);
            }
          }));
        });
    }
  }

  private MediaItem<?> getPlayableMediaItem() {
    return rootItem.getData() instanceof Serie ? episodesPresentation.episodeItem.get() : rootItem;
  }
}

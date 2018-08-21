package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
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
import hs.mediasystem.util.javafx.Binds;
import hs.mediasystem.util.javafx.Val;
import hs.mediasystem.util.javafx.action.Action;
import hs.mediasystem.util.javafx.action.SimpleAction;
import hs.mediasystem.util.javafx.property.SimpleReadOnlyObjectProperty;

import java.util.EnumMap;
import java.util.List;
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

  private final ObjectProperty<VideoLink> trailerVideoLink = new SimpleObjectProperty<>();

  public final MediaItem<?> rootItem;

  // TODO actions, can still keep references to UI elements if bound or listened to
  public final PlayAction play;
  public final ResumeAction resume;
  public final Action playTrailer = new SimpleAction("Trailer", trailerVideoLink.isNotNull(), this::playTrailer);

  private final Model model;

  private ProductionPresentation(
    Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider,
    VideoDatabase videoDatabase,
    StreamStateService streamStateService,
    Provider<EpisodesPresentation> episodesPresentationProvider,
    PlayAction.Factory playActionFactory,
    ResumeAction.Factory resumeActionFactory,
    MediaItem<?> rootItem
  ) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;
    this.videoDatabase = videoDatabase;
    this.streamStateService = streamStateService;
    this.rootItem = rootItem;

    EpisodesPresentation episodesPresentation = rootItem.getData() instanceof Serie ? episodesPresentationProvider.get().set((MediaItem<Serie>)rootItem) : null;

    this.model = new Model(rootItem, episodesPresentation == null ? null : episodesPresentation.episodeItems);
    this.play = playActionFactory.create(model.episodeOrMovieItem);
    this.resume = resumeActionFactory.create(model.episodeOrMovieItem);

    if(episodesPresentation != null) {
      this.model.episodeItem.bindBidirectional(episodesPresentation.episodeItem);
    }
  }

  public static class Model {
    private final ObjectProperty<State> internalState = new SimpleObjectProperty<>(State.OVERVIEW);
    private final ObjectProperty<ButtonState> internalButtonState = new SimpleObjectProperty<>(ButtonState.MAIN);

    public final ReadOnlyObjectProperty<State> state = new SimpleReadOnlyObjectProperty<>(internalState);
    public final ReadOnlyObjectProperty<ButtonState> buttonState = new SimpleReadOnlyObjectProperty<>(internalButtonState);

    public final List<MediaItem<Episode>> episodeItems;
    public final ObjectProperty<MediaItem<Episode>> episodeItem = new SimpleObjectProperty<>();

    public final Val<MediaItem<?>> episodeOrMovieItem;

    public Model(MediaItem<?> rootItem, List<MediaItem<Episode>> episodeItems) {
      this.episodeItems = episodeItems;
      this.episodeOrMovieItem = episodeItems == null ? Binds.monadic(rootItem) : (Val<MediaItem<?>>)(Val<?>)Binds.monadic(episodeItem);
    }
  }

  public Model createModel() {
    Model m = new Model(rootItem, model.episodeItems);

    bindModel(m);

    return m;
  }

  private void bindModel(Model other) {
    other.internalState.bindBidirectional(model.internalState);
    other.internalButtonState.bindBidirectional(model.internalButtonState);
    other.episodeItem.bindBidirectional(model.episodeItem);
  }

  @Override
  public void navigateBack(Event e) {
    switch(model.buttonState.get()) {
    case PLAY_RESUME:
    case RELATED:
      model.internalButtonState.set(ButtonState.MAIN);
      break;
    case MAIN:
      switch(model.state.get()) {
      case OVERVIEW:
        return;
      case LIST:
        model.internalState.set(State.OVERVIEW);
        break;
      case EPISODE:
        model.internalState.set(State.LIST);
        break;
      }
    }

    e.consume();
  }

  public void toEpisodeState() {
    if(this.model.episodeItem.get() == null) {
      throw new IllegalStateException("Cannot go to Episode state without an episode set");
    }

    update();

    this.model.internalState.set(State.EPISODE);
  }

  public void toListState() {
    if(model.episodeItems == null) {
      throw new IllegalStateException("Cannot go to List state if root item is not a Serie");
    }

    this.model.internalState.set(State.LIST);
  }

  public void toPlayResumeButtonState() {
    this.model.internalButtonState.set(ButtonState.PLAY_RESUME);
  }

  public void toRelatedButtonState() {
    this.model.internalButtonState.set(ButtonState.RELATED);
  }

  @Expose
  public void toggleWatchedState() {
    if(model.internalState.get() == State.LIST) {
      MediaItem<?> mediaItem = model.episodeItem.get();

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
    if(model.internalState.get() == State.LIST) {
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
    MediaItem<?> mediaItem = model.episodeItem.get();

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
    return rootItem.getData() instanceof Serie ? model.episodeItem.get() : rootItem;
  }
}

package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.mediamanager.MediaService;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.javafx.action.Action;
import hs.mediasystem.util.javafx.action.SimpleAction;
import hs.mediasystem.util.javafx.property.SimpleReadOnlyObjectProperty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.reactfx.EventSource;
import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public class ProductionPresentation extends AbstractPresentation implements Navigable {

  @Singleton
  public static class Factory {
    @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
    @Inject private VideoDatabase videoDatabase;
    @Inject private StreamStateService streamStateService;
    @Inject private Provider<EpisodesPresentation> episodesPresentationProvider;
    @Inject private PlayAction.Factory playActionFactory;
    @Inject private ResumeAction.Factory resumeActionFactory;
    @Inject private MediaItem.Factory mediaItemFactory;
    @Inject private MediaService mediaService;

    public ProductionPresentation create(MediaItem<?> mediaItem) {
      if(mediaItem.getData().getClass().equals(Production.class) || (mediaItem.getData() instanceof Serie && ((Serie)mediaItem.getData()).getSeasons() == null)) {  // If not a subclass of Production, or an incomplete Serie, get more details
        mediaItem = mediaItemFactory.create(videoDatabase.queryProduction(mediaItem.getProduction().getIdentifier()), mediaItem.getParent());
      }
      else if(mediaItem.getData() instanceof Serie) {
        mediaItem = mediaItemFactory.create(mediaService.toLocalSerie((Serie)mediaItem.getData()), mediaItem.getParent());
      }

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

  private final ObjectProperty<VideoLink> trailerVideoLink = new SimpleObjectProperty<>();
  private final ObjectProperty<State> internalState = new SimpleObjectProperty<>(State.OVERVIEW);
  private final ObjectProperty<ButtonState> internalButtonState = new SimpleObjectProperty<>(ButtonState.MAIN);

  public final ReadOnlyObjectProperty<State> state = new SimpleReadOnlyObjectProperty<>(internalState);
  public final ReadOnlyObjectProperty<ButtonState> buttonState = new SimpleReadOnlyObjectProperty<>(internalButtonState);

  public final List<MediaItem<Episode>> episodeItems;
  public final ObjectProperty<MediaItem<Episode>> episodeItem = new SimpleObjectProperty<>();

  public final MediaItem<?> rootItem;

  public final PlayAction play;
  public final ResumeAction resume;
  public final Action playTrailer = new SimpleAction("Trailer", trailerVideoLink.isNotNull(), this::playTrailer);
  public final EventSource<Event> showInfo = new EventSource<>();

  private final Var<Float> seasonWatchedFraction = Var.newSimpleVar(0.0f);  // If not empty, represents fraction of season watched
  private final Val<Boolean> episodeWatched;    // If not empty, represent if current episode is watched

  public final Val<Integer> totalDuration;
  public final Val<Double> watchedPercentage;  // Of top level item (Movie or Serie)
  public final Val<Double> missingFraction;    // Of top level item (Serie only)

  private final EpisodesPresentation episodesPresentation; // prevent gc

  @SuppressWarnings("unchecked")
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
    this.rootItem = rootItem;

    this.episodesPresentation = rootItem.getData() instanceof Serie ? episodesPresentationProvider.get().set((MediaItem<Serie>)rootItem) : null;
    this.episodeItems = episodesPresentation == null ? null : episodesPresentation.episodeItems;

    @SuppressWarnings("unchecked")
    Val<MediaItem<?>> episodeOrMovieItem = episodeItems == null ? Val.constant(rootItem) : (Val<MediaItem<?>>)(Val<?>)Val.wrap(episodeItem);

    this.play = playActionFactory.create(episodeOrMovieItem);
    this.resume = resumeActionFactory.create(episodeOrMovieItem);

    if(episodesPresentation != null) {
      this.episodeItem.bindBidirectional(episodesPresentation.episodeItem);
      this.episodeItem.addListener(obs -> internalButtonState.set(ButtonState.MAIN));
    }

    this.episodeWatched = Val.wrap(episodeItem).filter(mi -> !mi.getStreams().isEmpty()).flatMap(mi -> mi.watched);
    this.episodeWatched.addListener(obs -> updateSeasonWatchedFraction());

    this.totalDuration = episodeOrMovieItem
      .filter(mi -> !mi.getStreams().isEmpty())
      .flatMap(mi -> streamStateService.totalDurationProperty(mi.getStream().getId()))
      .filter(d -> d != -1);

    this.watchedPercentage = Val.create(this::getWatchedPercentage, EventStreams.merge(
      episodeWatched.changes(),
      seasonWatchedFraction.changes(),
      EventStreams.changesOf(rootItem.watched),
      EventStreams.changesOf(rootItem.missing),
      resume.resumePosition.changes(),
      totalDuration.changes()
    ));

    this.missingFraction = Val.create(this::getMissingFraction, EventStreams.merge(
      episodeWatched.changes(),
      seasonWatchedFraction.changes(),
      EventStreams.changesOf(rootItem.watched),
      EventStreams.changesOf(rootItem.missing)
    ));
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
        internalState.set(State.OVERVIEW);
        break;
      case EPISODE:
        internalState.set(State.LIST);
        break;
      }
    }

    e.consume();
  }

  public void showInfo(Event e) {
    showInfo.push(e);
  }

  public void toEpisodeState() {
    if(this.episodeItem.get() == null) {
      throw new IllegalStateException("Cannot go to Episode state without an episode set");
    }

    update();

    this.internalState.set(State.EPISODE);
  }

  public void toListState() {
    if(episodeItems == null) {
      throw new IllegalStateException("Cannot go to List state if root item is not a Serie");
    }

    this.internalState.set(State.LIST);
  }

  public void toPlayResumeButtonState() {
    this.internalButtonState.set(ButtonState.PLAY_RESUME);
  }

  public void toRelatedButtonState() {
    this.internalButtonState.set(ButtonState.RELATED);
  }

  /**
   * Returns a value indicating whether the item is watched, partially watched, unwatched or missing.
   *
   * A value of 1.0 indicates a watched item.
   * A value between 0.0 (exclusively) and 1.0 (exclusively) indicates a partially watched item.
   * A value of 0.0 indicates an unwatched item.
   * A negative value indicates a missing item.
   *
   * @return a value indicating whether the item is watched, partially watched, unwatched or missing
   */
  private double getWatchedPercentage() {
    if(rootItem.watched.get()) {
      return 1.0;
    }
    if(rootItem.missing.get()) {
      return -1.0;
    }
    if(!(rootItem.getData() instanceof Serie) && resume.resumePosition.isPresent() && totalDuration.isPresent()) {
      return resume.resumePosition.getValue() / (double)totalDuration.getValue();
    }
    if(rootItem.getData() instanceof Serie) {
      long totalWatched = episodeItems.stream()
        .filter(i -> i.getData().getSeasonNumber() != 0)
        .filter(i -> i.watched.get())
        .count();

      long total = episodeItems.stream()
        .filter(i -> i.getData().getSeasonNumber() != 0)
        .count();

      return totalWatched / (double)total;
    }

    return 0.0;
  }

  private double getMissingFraction() {
    if(rootItem.getData() instanceof Serie && !rootItem.watched.get() && !rootItem.missing.get()) {
      long totalMissingUnwatched = episodeItems.stream()
        .filter(i -> i.getData().getSeasonNumber() != 0)
        .filter(i -> i.missing.get() && !i.watched.get())
        .count();

      long total = episodeItems.stream()
        .filter(i -> i.getData().getSeasonNumber() != 0)
        .count();

      return totalMissingUnwatched / (double)total;
    }

    return 0.0;
  }

  public BooleanProperty watchedProperty() {
    if(!(rootItem.getData() instanceof Serie) && !rootItem.getStreams().isEmpty()) {
      return rootItem.watched;
    }

    return null;  // Indicates no state possible as there is no stream or is a serie
  }

  public BooleanProperty episodeWatchedProperty() {
    if(internalState.get() != State.OVERVIEW) {
      MediaItem<?> mediaItem = episodeItem.get();

      if(mediaItem != null && !mediaItem.getStreams().isEmpty()) {
        return mediaItem.watched;
      }
    }

    return null;  // Indicates no state possible as there is no stream
  }

  private void updateSeasonWatchedFraction() {
    MediaItem<Episode> currentItem = episodeItem.get();

    if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
      seasonWatchedFraction.setValue(null);
    }
    else {
      long total = 0;
      long watched = 0;
      int seasonNumber = currentItem.getData().getSeasonNumber();

      for(MediaItem<Episode> episode : episodeItems) {
        if(episode.getData().getSeasonNumber() == seasonNumber) {
          total++;

          if(episode.watched.get()) {
            watched++;
          }
        }
      }

      seasonWatchedFraction.setValue(watched / (float)total);
    }
  }

  public Property<Boolean> seasonWatchedProperty() {
    MediaItem<Episode> currentItem = episodeItem.get();

    if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
      return null;
    }

    List<MediaItem<Episode>> seasonEpisodes;
    List<MediaItem<Episode>> initialWatchedEpisodes;

    seasonEpisodes = episodeItems.stream()
      .filter(mi -> mi.getData().getSeasonNumber() == currentItem.getData().getSeasonNumber())
      .collect(Collectors.toList());

    initialWatchedEpisodes = seasonEpisodes.stream()
      .filter(mi -> mi.watched.get())
      .collect(Collectors.toList());

    Property<Boolean> seasonWatchedProperty = new SimpleObjectProperty<>();

    seasonWatchedProperty.setValue(initialWatchedEpisodes.isEmpty() ? Boolean.FALSE :
             initialWatchedEpisodes.size() == seasonEpisodes.size() ? Boolean.TRUE : null);

    seasonWatchedProperty.addListener((obs, old, current) -> {
      for(MediaItem<Episode> episode : seasonEpisodes) {
        if(current == null) {
          episode.watched.set(initialWatchedEpisodes.contains(episode));
        }
        else if(current == Boolean.TRUE) {
          episode.watched.set(true);
        }
        else {
          episode.watched.set(false);
        }
      }

      updateSeasonWatchedFraction();
    });

    return seasonWatchedProperty;
  }

  private void playTrailer(Event event) {
    VideoLink videoLink = trailerVideoLink.get();
    Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationProvider.get().set(getPlayableMediaItem(), new StringURI("https://www.youtube.com/watch?v=" + videoLink.getKey()), 0)));
  }

  public void update() {
    MediaItem<?> mediaItem = getMediaItemForTrailer();

    trailerVideoLink.set(null);

    if(mediaItem != null) {
      CompletableFuture.supplyAsync(() -> videoDatabase.queryVideoLinks(mediaItem.getProduction().getIdentifier()))
        .thenAccept(videoLinks -> {
          videoLinks.stream().filter(vl -> vl.getType() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> Platform.runLater(() -> {
            if(mediaItem.equals(getMediaItemForTrailer())) {
              trailerVideoLink.set(videoLink);
            }
          }));
        });
    }
  }

  private MediaItem<?> getMediaItemForTrailer() {
    if(internalState.get() == State.OVERVIEW) {
      return rootItem;
    }
    if(internalState.get() == State.EPISODE) {
      return episodeItem.get();
    }

    return null;
  }

  private MediaItem<?> getPlayableMediaItem() {
    return rootItem.getData() instanceof Serie ? episodeItem.get() : rootItem;
  }

  public Map<Integer, Var<SeasonWatchState>> getSeasonWatchStates() {
    return episodesPresentation.seasonWatchStates;
  }
}

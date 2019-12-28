package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;
import hs.mediasystem.mediamanager.db.VideoDatabase;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.javafx.action.Action;
import hs.mediasystem.util.javafx.action.SimpleAction;
import hs.mediasystem.util.javafx.property.SimpleReadOnlyObjectProperty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.application.Platform;
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
  private static final MediaType SERIE = MediaType.of("SERIE");

  @Singleton
  public static class Factory {
    @Inject private Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider;
    @Inject private VideoDatabase videoDatabase;
    @Inject private StreamStateService streamStateService;
    @Inject private Provider<EpisodesPresentation> episodesPresentationProvider;
    @Inject private PlayAction.Factory playActionFactory;
    @Inject private ResumeAction.Factory resumeActionFactory;
    @Inject private WorkService workService;

    public ProductionPresentation create(WorkId id) {
      return create(workService.find(id).orElseThrow());
    }

    private ProductionPresentation create(Work work) {
      return new ProductionPresentation(
        playbackOverlayPresentationProvider,
        videoDatabase,
        streamStateService,
        episodesPresentationProvider,
        playActionFactory,
        resumeActionFactory,
        work,
        workService.findChildren(work.getId())
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

  public final List<Work> episodeItems;
  public final ObjectProperty<Work> episodeItem = new SimpleObjectProperty<>();

  public final Work rootItem;

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

  private final Var<Boolean> rootWatched;
  private final Var<Boolean> rootMissing;

  private final StreamStateService streamStateService;

  private ProductionPresentation(
    Provider<PlaybackOverlayPresentation> playbackOverlayPresentationProvider,
    VideoDatabase videoDatabase,
    StreamStateService streamStateService,
    Provider<EpisodesPresentation> episodesPresentationProvider,
    PlayAction.Factory playActionFactory,
    ResumeAction.Factory resumeActionFactory,
    Work rootItem,
    List<Work> children
  ) {
    this.playbackOverlayPresentationProvider = playbackOverlayPresentationProvider;
    this.videoDatabase = videoDatabase;
    this.streamStateService = streamStateService;
    this.rootItem = rootItem;

    this.episodesPresentation = rootItem.getType().equals(SERIE) ? episodesPresentationProvider.get().set(rootItem, children) : null;
    this.episodeItems = episodesPresentation == null ? null : episodesPresentation.episodeItems;

    Val<Work> episodeOrMovieItem = episodeItems == null ? Val.constant(rootItem) : Val.wrap(episodeItem);

    this.play = playActionFactory.create(episodeOrMovieItem);
    this.resume = resumeActionFactory.create(episodeOrMovieItem);

    if(episodesPresentation != null) {
      this.episodeItem.bindBidirectional(episodesPresentation.episodeItem);
      this.episodeItem.addListener(obs -> internalButtonState.set(ButtonState.MAIN));
    }

    this.episodeWatched = Val.wrap(episodeItem).flatMap(r -> isWatched(r));
    this.episodeWatched.addListener(obs -> updateSeasonWatchedFraction());

    this.totalDuration = episodeOrMovieItem.map(r -> r.getPrimaryStream()
      .flatMap(ms -> ms.getMetaData().map(StreamMetaData::getLength).map(d -> (int)d.toSeconds()))
      .orElse(null));

    this.rootWatched = Var.newSimpleVar(rootItem.getState().isWatched());
    this.rootMissing = Var.newSimpleVar(rootItem.getStreams().isEmpty());

    this.watchedPercentage = Val.create(this::getWatchedPercentage, EventStreams.merge(
      episodeWatched.changes(),
      seasonWatchedFraction.changes(),
      EventStreams.changesOf(rootWatched),
      EventStreams.changesOf(rootMissing),
      resume.resumePosition.changes(),
      totalDuration.changes()
    ));

    this.missingFraction = Val.create(this::getMissingFraction, EventStreams.merge(
      episodeWatched.changes(),
      seasonWatchedFraction.changes(),
      EventStreams.changesOf(rootWatched),
      EventStreams.changesOf(rootMissing)
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
    if(rootWatched.getValue()) {
      return 1.0;
    }
    if(rootMissing.getValue()) {
      return -1.0;
    }
    if(!rootItem.getType().equals(SERIE) && resume.resumePosition.isPresent() && totalDuration.isPresent()) {
      return resume.resumePosition.getValue() / (double)totalDuration.getValue();
    }
    if(rootItem.getType().equals(SERIE)) {
      long totalWatched = episodeItems.stream()
        .filter(i -> ((Episode)i.getDescriptor()).getSeasonNumber() > 0)
        .filter(i -> isWatched(i).getValue())
        .count();

      long total = episodeItems.stream()
        .filter(i -> ((Episode)i.getDescriptor()).getSeasonNumber() > 0)
        .count();

      return totalWatched / (double)total;
    }

    return 0.0;
  }

  private double getMissingFraction() {
    if(rootItem.getType().equals(SERIE) && !rootWatched.getValue() && !rootMissing.getValue()) {
      long totalMissingUnwatched = episodeItems.stream()
        .filter(i -> ((Episode)i.getDescriptor()).getSeasonNumber() > 0)
        .filter(i -> i.getStreams().isEmpty() && !isWatched(i).getValue())
        .count();

      long total = episodeItems.stream()
        .filter(i -> ((Episode)i.getDescriptor()).getSeasonNumber() > 0)
        .count();

      return totalMissingUnwatched / (double)total;
    }

    return 0.0;
  }

  public Var<Boolean> watchedProperty() {
    if(!(rootItem.getType().equals(SERIE)) && !rootItem.getStreams().isEmpty()) {
      return rootWatched;
    }

    return null;  // Indicates no state possible as there is no stream or is a serie
  }

  public Var<Boolean> episodeWatchedProperty() {
    if(internalState.get() != State.OVERVIEW) {
      Work work = episodeItem.get();

      if(work != null && !work.getStreams().isEmpty()) {
        return isWatched(work);
      }
    }

    return null;  // Indicates no state possible as there is no stream
  }

  private void updateSeasonWatchedFraction() {
    Work currentItem = episodeItem.get();

    if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
      seasonWatchedFraction.setValue(null);
    }
    else {
      long total = 0;
      long watched = 0;
      int seasonNumber = ((Episode)currentItem.getDescriptor()).getSeasonNumber();

      for(Work episode : episodeItems) {
        if(((Episode)episode.getDescriptor()).getSeasonNumber() == seasonNumber) {
          total++;

          if(isWatched(episode).getValue()) {
            watched++;
          }
        }
      }

      seasonWatchedFraction.setValue(watched / (float)total);
    }
  }

  private Var<Boolean> isWatched(Work episode) {
    return episode.getPrimaryStream()
      .map(MediaStream::getId)
      .map(streamStateService::watchedProperty)
      .map(Var::suspendable)
      .orElse(Var.newSimpleVar(episode.getState().isWatched()).suspendable());
  }

  public Property<Boolean> seasonWatchedProperty() {
    Work currentItem = episodeItem.get();

    if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
      return null;
    }

    List<Work> seasonEpisodes;
    List<Work> initialWatchedEpisodes;

    seasonEpisodes = episodeItems.stream()
      .filter(r -> ((Episode)r.getDescriptor()).getSeasonNumber() == ((Episode)currentItem.getDescriptor()).getSeasonNumber())
      .collect(Collectors.toList());

    initialWatchedEpisodes = seasonEpisodes.stream()
      .filter(r -> isWatched(r).getValue())
      .collect(Collectors.toList());

    Property<Boolean> seasonWatchedProperty = new SimpleObjectProperty<>();

    seasonWatchedProperty.setValue(initialWatchedEpisodes.isEmpty() ? Boolean.FALSE :
             initialWatchedEpisodes.size() == seasonEpisodes.size() ? Boolean.TRUE : null);

    seasonWatchedProperty.addListener((obs, old, current) -> {
      for(Work episode : seasonEpisodes) {
        Var<Boolean> watched = isWatched(episode);

        if(current == null) {
          watched.setValue(initialWatchedEpisodes.contains(episode));
        }
        else if(current == Boolean.TRUE) {
          watched.setValue(true);
        }
        else {
          watched.setValue(false);
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
    Work work = getWorkForTrailer();

    trailerVideoLink.set(null);

    if(work != null) {
      CompletableFuture.supplyAsync(() -> videoDatabase.queryVideoLinks(((ProductionIdentifier)work.getId().getIdentifier())))
        .thenAccept(videoLinks -> {
          videoLinks.stream().filter(vl -> vl.getType() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> Platform.runLater(() -> {
            if(work.equals(getWorkForTrailer())) {
              trailerVideoLink.set(videoLink);
            }
          }));
        });
    }
  }

  private Work getWorkForTrailer() {
    if(internalState.get() == State.OVERVIEW) {
      return rootItem;
    }
    if(internalState.get() == State.EPISODE) {
      return episodeItem.get();
    }

    return null;
  }

  private Work getPlayableMediaItem() {
    return rootItem.getType().equals(SERIE) ? episodeItem.get() : rootItem;
  }

  public Map<Integer, Var<SeasonWatchState>> getSeasonWatchStates() {
    return episodesPresentation.seasonWatchStates;
  }
}

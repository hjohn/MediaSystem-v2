package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.presentation.AbstractPresentation;
import hs.mediasystem.runner.Navigable;
import hs.mediasystem.runner.NavigateEvent;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.javafx.action.Action;
import hs.mediasystem.util.javafx.action.SimpleAction;
import hs.mediasystem.util.javafx.property.SimpleReadOnlyObjectProperty;

import java.time.Duration;
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
    @Inject private PlaybackOverlayPresentation.Factory playbackOverlayPresentationFactory;
    @Inject private Provider<EpisodesPresentation> episodesPresentationProvider;
    @Inject private PlayAction.Factory playActionFactory;
    @Inject private ResumeAction.Factory resumeActionFactory;
    @Inject private WorkClient workClient;

    public ProductionPresentation create(WorkId id, State state, WorkId childId) {
      return create(workClient.find(id).orElseThrow(), state, childId);
    }

    public ProductionPresentation create(WorkId id) {
      return create(id, State.OVERVIEW, null);
    }

    private ProductionPresentation create(Work work, State state, WorkId childId) {
      return new ProductionPresentation(
        playbackOverlayPresentationFactory,
        episodesPresentationProvider,
        playActionFactory,
        resumeActionFactory,
        workClient,
        work,
        workClient.findChildren(work.getId()),
        state,
        childId
      );
    }
  }

  public enum State {
    OVERVIEW, LIST, EPISODE
  }

  public enum ButtonState {
    MAIN, PLAY_RESUME, RELATED
  }

  private final PlaybackOverlayPresentation.Factory playbackOverlayPresentationFactory;
  private final WorkClient workClient;

  private final ObjectProperty<VideoLink> trailerVideoLink = new SimpleObjectProperty<>();
  private final ObjectProperty<State> internalState = new SimpleObjectProperty<>(State.OVERVIEW);
  private final ObjectProperty<ButtonState> internalButtonState = new SimpleObjectProperty<>(ButtonState.MAIN);

  public final ReadOnlyObjectProperty<State> state = new SimpleReadOnlyObjectProperty<>(internalState);
  public final ReadOnlyObjectProperty<ButtonState> buttonState = new SimpleReadOnlyObjectProperty<>(internalButtonState);

  public final List<Work> episodeItems;
  public final Var<Work> episodeItem = Var.newSimpleVar(null);

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

  private ProductionPresentation(
    PlaybackOverlayPresentation.Factory playbackOverlayPresentationFactory,
    Provider<EpisodesPresentation> episodesPresentationProvider,
    PlayAction.Factory playActionFactory,
    ResumeAction.Factory resumeActionFactory,
    WorkClient workClient,
    Work rootItem,
    List<Work> children,
    State initialState,
    WorkId childId
  ) {
    this.internalState.set(initialState);
    this.playbackOverlayPresentationFactory = playbackOverlayPresentationFactory;
    this.workClient = workClient;
    this.rootItem = rootItem;

    this.episodesPresentation = rootItem.getType().equals(SERIE) ? episodesPresentationProvider.get().set(rootItem, children) : null;
    this.episodeItems = episodesPresentation == null ? null : episodesPresentation.episodeItems;

    Val<Work> episodeOrMovieItem = episodeItems == null ? Val.constant(rootItem) : Val.wrap(episodeItem);

    this.play = playActionFactory.create(episodeOrMovieItem);
    this.resume = resumeActionFactory.create(episodeOrMovieItem);

    if(episodesPresentation != null) {
      this.episodeItem.bindBidirectional(episodesPresentation.episodeItem);
      this.episodeItem.addListener(obs -> internalButtonState.set(ButtonState.MAIN));

      if(childId != null) {
        for(Work episode : episodeItems) {
          if(episode.getId().equals(childId)) {
            this.episodeItem.setValue(episode);
            break;
          }
        }
      }
    }

    this.episodeWatched = Val.wrap(episodeItem).flatMap(r -> isWatched(r));
    this.episodeWatched.addListener(obs -> updateSeasonWatchedFraction());

    this.totalDuration = episodeOrMovieItem.map(r -> r.getPrimaryStream()
      .flatMap(ms -> ms.getMetaData().map(StreamMetaData::getLength).map(d -> (int)d.toSeconds()))
      .orElse(null));

    this.rootWatched = rootItem.getState().isConsumed();
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
    if(this.episodeItem.getValue() == null) {
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

  public void toMainButtonState() {
    this.internalButtonState.set(ButtonState.MAIN);
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
      return resume.resumePosition.getValue().toSeconds() / (double)totalDuration.getValue();
    }
    if(rootItem.getType().equals(SERIE)) {
      long totalWatched = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .filter(i -> isWatched(i).getValue())
        .count();

      long total = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .count();

      return totalWatched / (double)total;
    }

    return 0.0;
  }

  private double getMissingFraction() {
    if(rootItem.getType().equals(SERIE) && !rootWatched.getValue() && !rootMissing.getValue()) {
      long totalMissingUnwatched = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
        .filter(i -> i.getStreams().isEmpty() && !isWatched(i).getValue())
        .count();

      long total = episodeItems.stream()
        .filter(i -> i.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) > 0)
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
      Work work = episodeItem.getValue();

      if(work != null && !work.getStreams().isEmpty()) {
        return isWatched(work);
      }
    }

    return null;  // Indicates no state possible as there is no stream
  }

  private void updateSeasonWatchedFraction() {
    Work currentItem = episodeItem.getValue();

    if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
      seasonWatchedFraction.setValue(null);
    }
    else {
      long total = 0;
      long watched = 0;
      int seasonNumber = currentItem.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0);

      for(Work episode : episodeItems) {
        if(episode.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) == seasonNumber) {
          total++;

          if(isWatched(episode).getValue()) {
            watched++;
          }
        }
      }

      seasonWatchedFraction.setValue(watched / (float)total);
    }
  }

  private static Var<Boolean> isWatched(Work episode) {
    return episode.getState().isConsumed();
  }

  public Property<Boolean> seasonWatchedProperty() {
    Work currentItem = episodeItem.getValue();

    if(internalState.get() == State.OVERVIEW || currentItem == null || currentItem.getStreams().isEmpty()) {
      return null;
    }

    List<Work> seasonEpisodes;
    List<Work> initialWatchedEpisodes;

    seasonEpisodes = episodeItems.stream()
      .filter(w -> w.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0) == currentItem.getDetails().getSequence().flatMap(Sequence::getSeasonNumber).orElse(0))
      .collect(Collectors.toList());

    initialWatchedEpisodes = seasonEpisodes.stream()
      .filter(w -> isWatched(w).getValue())
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
    Event.fireEvent(event.getTarget(), NavigateEvent.to(playbackOverlayPresentationFactory.create(rootItem, new StringURI("https://www.youtube.com/watch?v=" + videoLink.getKey()), Duration.ZERO)));
  }

  public void update() {
    trailerVideoLink.set(null);

    CompletableFuture.supplyAsync(() -> workClient.findVideoLinks(rootItem.getId()))
      .thenAccept(videoLinks -> {
        videoLinks.stream().filter(vl -> vl.getType() == VideoLink.Type.TRAILER).findFirst().ifPresent(videoLink -> Platform.runLater(() -> trailerVideoLink.set(videoLink)));
      });
  }

  public Map<Integer, Var<SeasonWatchState>> getSeasonWatchStates() {
    return episodesPresentation.seasonWatchStates;
  }
}

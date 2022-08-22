package hs.mediasystem.db.services;

import hs.mediasystem.db.base.StreamState;
import hs.mediasystem.db.base.StreamStateProvider;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Recommendation;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationService {
  @Inject private WorksService worksService;
  @Inject private WorkService workService;
  @Inject private LinkedResourcesService linkedResourcesService;
  @Inject private LinkedWorksService linkedWorksService;
  @Inject private StreamStateProvider streamStateProvider;
  @Inject private MediaStreamService mediaStreamService;

  /**
   * Finds recommendations on what to continue watching or what to watch next.
   *
   * @param maximum the maximum amount of items to return
   * @return a {@link List} of {@link Recommendation}s, never null but can be empty
   */
  public List<Recommendation> findRecommendations(int maximum) {
    // TODO this should really be backed by a db query; as it is, it will walk through all stream states
    return streamStateProvider.map(stream -> stream
      .map(StreamState::getContentID)
      .flatMap(cid -> linkedResourcesService.findFirst(cid).map(mediaStreamService::toMediaStream).stream())
      .filter(ms -> ms.state().lastConsumptionTime().isPresent())  // this check could be done earlier as stream state should have this information already
      .sorted(Comparator.comparing((MediaStream ms) -> ms.state().lastConsumptionTime().orElseThrow()).reversed())
      .map(this::toPartiallyWatchedOrNextUnwatchedRecommendation)
      .flatMap(Optional::stream)
      .filter(r -> r.work().getType().isPlayable())  // doubtful this check does anything at this point
      .limit(maximum)
      .collect(Collectors.toList())
    );
  }

  /**
   * Finds new items added to the system.
   *
   * @param filter a filter, cannot be null
   * @return a {@link List} of {@link Recommendation}s, never null but can be empty
   */
  public List<Recommendation> findNew(Predicate<MediaType> filter) {
    return worksService.findNewest(200, filter).stream()
      .map(this::toNewRecommendation)
      .collect(Collectors.toList());
  }

  private Recommendation toNewRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();

    return new Recommendation(stream.discoveryTime(), work);
  }

  private Optional<Recommendation> toPartiallyWatchedOrNextUnwatchedRecommendation(MediaStream stream) {
    StreamID parentId = stream.parentId().orElse(null);

    if(parentId == null) {
      return toProductionRecommendation(stream);
    }

    return toEpisodeRecommendation(stream);
  }

  private Optional<Recommendation> toProductionRecommendation(MediaStream stream) {
    State state = stream.state();
    Instant lastWatchedTime = state.lastConsumptionTime().orElseThrow();

    if(state.consumed()) {
      // TODO Must be a movie, find collection for "next"
    }
    else if(!state.resumePosition().isZero()) {  // Partially watched movie
      return workService.findFirst(stream.id())
        .map(w -> new Recommendation(lastWatchedTime, w));
    }

    return Optional.empty();
  }

  private Optional<Recommendation> toEpisodeRecommendation(MediaStream stream) {
    StreamID parentId = stream.parentId().orElseThrow(() -> new IllegalArgumentException("stream must represent an episode: " + stream));
    State state = stream.state();

    boolean watched = state.consumed();
    Duration position = state.resumePosition();
    Instant lastWatchedTime = state.lastConsumptionTime().orElseThrow();

    return findBestDescriptor(parentId).filter(Serie.class::isInstance).map(Serie.class::cast).map(serie -> {
      WorkDescriptor descriptor = findBestDescriptor(stream.id()).orElse(null);

      if(descriptor instanceof Episode episode && watched) {
        return serie.findNextEpisode(episode)
          .flatMap(nextEpisode -> linkedWorksService.find(nextEpisode.getId())  // Episode to Stream
            .map(mediaStreamService::toMediaStream)
            .map(ms -> !ms.state().consumed() && ms.state().resumePosition().isZero()
              ? workService.findFirst(ms.id()).map(w -> new Recommendation(lastWatchedTime, w)).orElse(null)
              : null
            )
          )
          .orElse(null);
      }

      if(!position.isZero()) {
        return workService.findFirst(stream.id())
          .map(w -> new Recommendation(lastWatchedTime, w))
          .orElse(null);
      }

      return null;
    });
  }

  private Optional<WorkDescriptor> findBestDescriptor(StreamID id) {
    return linkedWorksService.find(id).stream().map(lw -> lw.work().descriptor()).findFirst();
  }
}

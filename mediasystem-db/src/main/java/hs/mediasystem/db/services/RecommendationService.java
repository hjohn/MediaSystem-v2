package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Episode;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.domain.stream.Recommendation;
import hs.mediasystem.db.base.StreamState;
import hs.mediasystem.db.base.StreamStateProvider;
import hs.mediasystem.db.services.domain.LinkedResource;
import hs.mediasystem.domain.media.MediaStream;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.State;

import java.net.URI;
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
  @Inject private LocalWorkService localWorkService;
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
    return linkedWorksService.findNewest(200, filter).stream()
      .map(t -> new Recommendation(t.matchedResources().get(0).resource().discoveryTime(), localWorkService.toWork(t)))
      .collect(Collectors.toList());
  }

  private Optional<Recommendation> toPartiallyWatchedOrNextUnwatchedRecommendation(MediaStream stream) {
    LinkedResource parent = linkedResourcesService.findParent(stream.location()).orElse(null);

    return parent == null ? toProductionRecommendation(stream) : toEpisodeRecommendation(stream, parent);
  }

  private Optional<Recommendation> toProductionRecommendation(MediaStream stream) {
    State state = stream.state();
    Instant lastWatchedTime = state.lastConsumptionTime().orElseThrow();

    if(state.consumed()) {
      // TODO Must be a movie, find collection for "next"
    }
    else if(!state.resumePosition().isZero()) {  // Partially watched movie
      return localWorkService.findFirst(stream.location())
        .map(w -> new Recommendation(lastWatchedTime, w));
    }

    return Optional.empty();
  }

  private Optional<Recommendation> toEpisodeRecommendation(MediaStream stream, LinkedResource parent) {
    State state = stream.state();

    boolean watched = state.consumed();
    Duration position = state.resumePosition();
    Instant lastWatchedTime = state.lastConsumptionTime().orElseThrow();

    if(parent.works().get(0).descriptor() instanceof Serie serie) {  // Parent could also be a folder
      WorkDescriptor descriptor = findBestDescriptor(stream.location()).orElse(null);

      if(descriptor instanceof Episode episode && watched) {
        return serie.findNextEpisode(episode)
          .flatMap(nextEpisode -> linkedWorksService.find(nextEpisode.getId())  // Episode to Stream
            .map(mediaStreamService::toMediaStream)
            .map(ms -> !ms.state().consumed() && ms.state().resumePosition().isZero()
              ? localWorkService.findFirst(ms.location()).map(w -> new Recommendation(lastWatchedTime, w)).orElse(null)
              : null
            )
          );
      }

      if(!position.isZero()) {
        return localWorkService.findFirst(stream.location())
          .map(w -> new Recommendation(lastWatchedTime, w));
      }
    }

    return Optional.empty();
  }

  private Optional<WorkDescriptor> findBestDescriptor(URI location) {
    return linkedWorksService.find(location).stream().map(lw -> lw.work().descriptor()).findFirst();
  }
}

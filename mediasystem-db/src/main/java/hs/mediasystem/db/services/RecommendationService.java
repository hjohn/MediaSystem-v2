package hs.mediasystem.db.services;

import hs.mediasystem.db.base.StreamState;
import hs.mediasystem.db.base.StreamStateProvider;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Recommendation;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.mediamanager.StreamableStore;

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
  @Inject private StreamableStore streamStore;
  @Inject private DescriptorStore descriptorStore;
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
      .map(mediaStreamService::findFirst)
      .flatMap(Optional::stream)
      .filter(ms -> ms.getState().getLastConsumptionTime().isPresent())  // this check could be done earlier as stream state should have this information already
      .sorted(Comparator.comparing((MediaStream ms) -> ms.getState().getLastConsumptionTime().orElseThrow()).reversed())
      .map(this::toPartiallyWatchedOrNextUnwatchedRecommendation)
      .flatMap(Optional::stream)
      .filter(r -> r.getWork().getType().isPlayable())  // doubtful this check does anything at this point
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

    return new Recommendation(stream.getAttributes().getDiscoveryTime(), work);
  }

  private Optional<Recommendation> toPartiallyWatchedOrNextUnwatchedRecommendation(MediaStream stream) {
    StreamID parentId = stream.getParentId().orElse(null);

    if(parentId == null) {
      return toProductionRecommendation(stream);
    }

    return toEpisodeRecommendation(stream);
  }

  private Optional<Recommendation> toProductionRecommendation(MediaStream stream) {
    State state = stream.getState();
    Instant lastWatchedTime = state.getLastConsumptionTime().orElseThrow();

    if(state.isConsumed()) {
      // TODO Must be a movie, find collection for "next"
    }
    else if(!state.getResumePosition().isZero()) {  // Partially watched movie
      return workService.find(stream.getId())
        .map(w -> new Recommendation(lastWatchedTime, w));
    }

    return Optional.empty();
  }

  private Optional<Recommendation> toEpisodeRecommendation(MediaStream stream) {
    StreamID parentId = stream.getParentId().orElseThrow(() -> new IllegalArgumentException("stream must represent an episode: " + stream));
    State state = stream.getState();

    boolean watched = state.isConsumed();
    Duration position = state.getResumePosition();
    Instant lastWatchedTime = state.getLastConsumptionTime().orElseThrow();

    return findBestIdentifier(parentId).flatMap(
      identifier -> descriptorStore.find(identifier).filter(Serie.class::isInstance).map(Serie.class::cast).map(serie -> {
        Episode episode = (Episode)findBestDescriptor(stream.getId()).orElse(null);

        if(episode != null) {
          if(watched) {
            return serie.findNextEpisode(episode)
              .flatMap(nextEpisode -> streamStore.findStreams(nextEpisode.getIdentifier()).stream().findFirst()  // Episode to Stream
                .map(mediaStreamService::toMediaStream)
                .map(ms -> {
                  if(!ms.getState().isConsumed() && ms.getState().getResumePosition().isZero()) {
                    return workService.find(ms.getId())
                      .map(w -> new Recommendation(lastWatchedTime, w))
                      .orElse(null);
                  }

                  return null;
                })
              )
              .orElse(null);
          }
          else if(!position.isZero()) {
            return workService.find(stream.getId())
              .map(w -> new Recommendation(lastWatchedTime, w))
              .orElse(null);
          }
        }

        return null;
      })
    );
  }

  private Optional<Identifier> findBestIdentifier(StreamID id) {
    return streamStore.findIdentification(id)
      .map(Identification::getPrimaryIdentifier);
  }

  private Optional<MediaDescriptor> findBestDescriptor(StreamID id) {
    return findBestIdentifier(id)
      .flatMap(descriptorStore::find);
  }
}

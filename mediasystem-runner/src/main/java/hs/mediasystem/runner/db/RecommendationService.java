package hs.mediasystem.runner.db;

import hs.mediasystem.db.services.SerieHelper;
import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.db.services.WorksService;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.mediamanager.BasicStreamStore;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecommendationService {
  private static final MediaType SERIE = MediaType.of("SERIE");

  @Inject private WorksService worksService;
  @Inject private WorkService workService;
  @Inject private BasicStreamStore streamStore;  // Only stores top level items, not children
  @Inject private DescriptorStore descriptorStore;
  @Inject private SerieHelper serieHelper;

  public List<Recommendation> findRecommendations(int maximum) {
    return worksService.findLastWatched(maximum * 2, null).stream()    // Find twice as much matched last watched Works, as Works that are consecutive will get filtered and only last one is added
      .filter(r -> !r.getType().equals(SERIE))  // Donot turn Series into recommendations
      .map(this::toPartiallyWatchedOrNextUnwatchedRecommendation)
      .flatMap(Optional::stream)
      .limit(maximum)
      .collect(Collectors.toList());
  }

  public List<Recommendation> findNew() {
    return worksService.findNewest(100).stream()
      .map(this::toNewRecommendation)
      .flatMap(Optional::stream)
      .collect(Collectors.toList());
  }

  private Optional<Recommendation> toNewRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID streamId = stream.getId();
    State state = work.getState();
    Duration position = state.getResumePosition();
    Duration length = stream.getMetaData().map(StreamMetaData::getLength).orElse(null);
    boolean watched = state.isConsumed();

    return getBestIdentifier(streamId)
      .flatMap(descriptorStore::find)
      .map(descriptor -> new Recommendation(stream.getAttributes().getCreationTime(), work, null, descriptor, streamId, length, position, watched));
  }

  private Optional<Recommendation> toPartiallyWatchedOrNextUnwatchedRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID parentId = stream.getParentId().orElse(null);

    if(parentId == null) {
      return toProductionRecommendation(work);
    }

    return toEpisodeRecommendation(work);
  }

  private Optional<Recommendation> toProductionRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID streamId = stream.getId();
    State state = work.getState();
    Duration position = state.getResumePosition();
    Duration length = stream.getMetaData().map(StreamMetaData::getLength).orElse(null);
    Instant lastWatchedTime = state.getLastConsumptionTime().orElseThrow();
    boolean watched = state.isConsumed();

    return getBestIdentifier(streamId).flatMap(descriptorStore::find).map(descriptor -> {
      if(watched) {
        // TODO Must be a movie, find collection for "next"
      }
      else if(!position.isZero()) {  // Partially watched movie
        return new Recommendation(lastWatchedTime, work, null, descriptor, streamId, length, position, watched);
      }

      return null;
    });
  }

  private Optional<Recommendation> toEpisodeRecommendation(Work work) {
    MediaStream stream = work.getPrimaryStream().orElseThrow();
    StreamID parentId = stream.getParentId().orElseThrow(() -> new IllegalArgumentException("work must represent an episode: " + work));
    StreamID streamId = stream.getId();
    State state = work.getState();

    boolean watched = state.isConsumed();
    Duration position = state.getResumePosition();
    Duration length = stream.getMetaData().orElseThrow().getLength();
    Instant lastWatchedTime = state.getLastConsumptionTime().orElseThrow();

    return getBestIdentifier(parentId).flatMap(
      identifier -> descriptorStore.find(identifier).map(Serie.class::cast).map(serie -> {
        // Stream(Attributes) to Episode
        List<Episode> episodes = serieHelper.findChildDescriptors(serie, stream.getAttributes().getAttributes());

        if(!episodes.isEmpty()) {
          if(watched) {
            return serie.findNextEpisode(episodes.get(episodes.size() - 1))
              .flatMap(nextEpisode -> serieHelper.findChildStreams(identifier, nextEpisode.getIdentifier()).stream().findFirst()  // Episode to Stream
                .map(BasicStream::getId)
                .flatMap(workService::find)
                .map(r -> {
                  if(!r.getState().isConsumed() && r.getState().getResumePosition().isZero()) {
                    return new Recommendation(lastWatchedTime, r, serie, nextEpisode, r.getPrimaryStream().orElseThrow().getId(), null, r.getState().getResumePosition(), false);
                  }

                  return null;
                })
              )
              .orElse(null);
          }
          else if(!position.isZero()) {
            return new Recommendation(lastWatchedTime, work, serie, episodes.get(0), streamId, length, position, false);
          }
        }

        return null;
      })
    );
  }

  private Optional<Identifier> getBestIdentifier(StreamID streamId) {  // TODO use a specific type of identifier preferably?
    return streamStore.findIdentifications(streamId).keySet().stream().findFirst();
  }
}

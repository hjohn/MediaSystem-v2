package hs.mediasystem.runner.db;

import hs.mediasystem.db.resource.ResourceStore;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Resource;
import hs.mediasystem.ext.basicmediatypes.domain.stream.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
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

  @Inject private ResourceStore store;
  @Inject private BasicStreamStore streamStore;  // Only stores top level items, not children
  @Inject private DescriptorStore descriptorStore;
  @Inject private SerieHelper serieHelper;

  public List<Recommendation> findRecommendations(int maximum) {
    return store.findLastWatched(maximum * 2, null).stream()    // Find twice as matched last watched Resources, as Resources that are consecutive will get filtered and only last one is added
      .filter(r -> !r.getAttributes().getType().equals(SERIE))  // Donot turn Series into recommendations
      .map(this::toPartiallyWatchedOrNextUnwatchedRecommendation)
      .flatMap(Optional::stream)
      .limit(maximum)
      .collect(Collectors.toList());
  }

  public List<Recommendation> findNew() {
    return store.findNewest(100).stream()
      .map(this::toNewRecommendation)
      .flatMap(Optional::stream)
      .collect(Collectors.toList());
  }

  private Optional<Recommendation> toNewRecommendation(Resource resource) {
    StreamID streamId = resource.getId();
    State state = resource.getState();
    Duration position = state.getResumePosition();
    Duration length = resource.getMetaData().map(StreamMetaData::getLength).orElse(null);
    boolean watched = state.isWatched();

    return getBestIdentifier(streamId)
      .flatMap(descriptorStore::find)
      .map(descriptor -> new Recommendation(resource.getAttributes().getCreationTime(), null, descriptor, streamId, length, position, watched));
  }

  private Optional<Recommendation> toPartiallyWatchedOrNextUnwatchedRecommendation(Resource resource) {
    StreamID parentId = resource.getParentId().orElse(null);

    if(parentId == null) {
      return toProductionRecommendation(resource);
    }

    return toEpisodeRecommendation(resource);
  }

  private Optional<Recommendation> toProductionRecommendation(Resource resource) {
    StreamID streamId = resource.getId();
    State state = resource.getState();
    Duration position = state.getResumePosition();
    Duration length = resource.getMetaData().map(StreamMetaData::getLength).orElse(null);
    Instant lastWatchedTime = state.getLastWatchedTime().orElseThrow();
    boolean watched = state.isWatched();

    return getBestIdentifier(streamId).flatMap(descriptorStore::find).map(descriptor -> {
      if(watched) {
        // TODO Must be a movie, find collection for "next"
      }
      else if(!position.isZero()) {  // Partially watched movie
        return new Recommendation(lastWatchedTime, null, descriptor, streamId, length, position, watched);
      }

      return null;
    });
  }

  private Optional<Recommendation> toEpisodeRecommendation(Resource resource) {
    StreamID parentId = resource.getParentId().orElseThrow(() -> new IllegalArgumentException("resource must represent an episode: " + resource));
    StreamID streamId = resource.getId();
    State state = resource.getState();

    boolean watched = state.isWatched();
    Duration position = state.getResumePosition();
    Duration length = resource.getMetaData().orElseThrow().getLength();
    Instant lastWatchedTime = state.getLastWatchedTime().orElseThrow();

    return getBestIdentifier(parentId).flatMap(
      identifier -> descriptorStore.find(identifier).map(Serie.class::cast).map(serie -> {
        // Stream(Attributes) to Episode
        List<Episode> episodes = serieHelper.findChildDescriptors(serie, resource.getAttributes().getAttributes());

        if(!episodes.isEmpty()) {
          if(watched) {
            return serie.findNextEpisode(episodes.get(episodes.size() - 1))
              .flatMap(nextEpisode -> serieHelper.findChildStreams(identifier, nextEpisode.getIdentifier()).stream().findFirst()  // Episode to Stream
                .map(BasicStream::getId)
                .flatMap(store::find)
                .map(r -> {
                  if(!r.getState().isWatched() && r.getState().getResumePosition().isZero()) {
                    return new Recommendation(lastWatchedTime, serie, nextEpisode, r.getId(), null, r.getState().getResumePosition(), false);
                  }

                  return null;
                })
              )
              .orElse(null);
          }
          else if(!position.isZero()) {
            return new Recommendation(lastWatchedTime, serie, episodes.get(0), streamId, length, position, false);
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

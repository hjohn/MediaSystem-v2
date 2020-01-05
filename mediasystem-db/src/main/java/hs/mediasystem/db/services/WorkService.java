package hs.mediasystem.db.services;

import hs.mediasystem.db.DatabaseStreamStore;
import hs.mediasystem.db.StreamStateService;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.MediaStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Parent;
import hs.mediasystem.ext.basicmediatypes.domain.stream.State;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamAttributes;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;
import hs.mediasystem.ext.basicmediatypes.services.ProductionCollectionQueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.mediamanager.LocalSerie;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.MediaType;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.Tuple;
import hs.mediasystem.util.Tuple.Tuple2;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkService {
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType COLLECTION = MediaType.of("COLLECTION");

  @Inject private DatabaseStreamStore streamStore;
  @Inject private StreamStateService stateService;
  @Inject private DefaultStreamMetaDataStore metaDataStore;
  @Inject private DescriptorStore descriptorStore;
  @Inject private SerieHelper serieHelper;
  @Inject private List<QueryService> queryServices;
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<ProductionCollectionQueryService> productionCollectionQueryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;

  private static final List<String> dataSourcePriorities = List.of("TMDB", "LOCAL");
  private static final Comparator<Map.Entry<Identifier, Identification>> DATA_SOURCE_PRIORITY =
      Comparator.comparing((Map.Entry<Identifier, Identification> e) -> dataSourcePriorities.indexOf(e.getKey().getDataSource().getName()));
  private static final State UNWATCHED_STATE = new State(null, false, Duration.ZERO);

  public synchronized Optional<Work> find(StreamID streamId) {  // TODO needs to become private, called by Home screen recommendations, which should move to back-end
    return streamStore.findStream(streamId).map(this::toWork);
  }

  @SuppressWarnings("unchecked")
  private <T extends Production> T queryProduction(ProductionIdentifier identifier) {
    for(QueryService queryService : queryServices) {
      if(queryService.getDataSource().equals(identifier.getDataSource())) {
        return (T)queryService.query(identifier);
      }
    }

    return null;
  }

  public synchronized Optional<Work> find(WorkId workId) {
    if(workId.getIdentifier().getDataSource().getName().equals("STREAM_ID")) {
      return find(new StreamID(Integer.parseInt(workId.getIdentifier().getId())));
    }
    if(workId.getIdentifier().getDataSource().getType().equals(COLLECTION)) {
      return queryProductionCollection(workId.getIdentifier())
        .map(pc -> toWork(pc, null));
    }

    Identifier identifier = workId.getIdentifier();

    if(identifier.getRootIdentifier() != null) {
      Optional<MediaDescriptor> rootDescriptor = descriptorStore.find(identifier.getRootIdentifier())
        .or(() -> Optional.ofNullable(queryProduction((ProductionIdentifier)identifier.getRootIdentifier())));

      if(rootDescriptor.isPresent()) {
        Serie serie = (Serie)rootDescriptor.orElseThrow();

        Optional<Episode> episode = serie.getSeasons().stream()
          .map(Season::getEpisodes)
          .flatMap(Collection::stream)
          .filter(ep -> ep.getIdentifier().equals(identifier))
          .findFirst();

        if(episode.isPresent()) {
          return Optional.of(toWork(episode.orElseThrow(), rootDescriptor.orElseThrow()));
        }
      }
    }
    else {
      Optional<MediaDescriptor> descriptor = descriptorStore.find(identifier)
        .or(() -> Optional.ofNullable(queryProduction((ProductionIdentifier)identifier)));

      if(descriptor.isPresent()) {
        return Optional.of(toWork(descriptor.orElseThrow(), null));
      }
    }

    return Optional.empty();
  }

  public synchronized List<Work> findChildren(WorkId workId) {
    MediaType type = workId.getIdentifier().getDataSource().getType();

    if(type.equals(SERIE)) {

      /*
       * Case 1: Identifier is a special identifier identifying a stream directly
       * Case 2: Identifier is found in StreamStore
       * Case 3: Identifier is has no stream associated with it whatsoever
       */

      if(workId.getIdentifier().getDataSource().getName().equals("STREAM_ID")) {
        return findChildren(new StreamID(Integer.parseInt(workId.getIdentifier().getId())));
      }

      Set<BasicStream> streams = streamStore.findStreams(workId.getIdentifier());

      if(!streams.isEmpty()) {
        return findChildren(streams.stream().findFirst().get().getId());  // TODO if multiple Series have the same identifier, this picks one at random
      }

      return findChildren((ProductionIdentifier)workId.getIdentifier());
    }
    else if(type.equals(COLLECTION)) {
      return queryProductionCollection(workId.getIdentifier()).map(ProductionCollection::getItems).orElse(List.of()).stream()
        .map(p -> toWork(p, null))
        .collect(Collectors.toList());
    }

    return List.of();
  }

  // find children needs to always add all known children from the Serie, plus any that couldn't be matched
  // as extras!
  private synchronized List<Work> findChildren(StreamID streamId) {
    Serie serie = (Serie)findBestDescriptor(streamId).map(t -> t.a).get();
    LocalSerie localSerie = serieHelper.toLocalSerie(serie);

    List<Episode> episodes = localSerie.getSeasons().stream()
      .map(Season::getEpisodes)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());

    episodes.addAll(localSerie.getExtras());

    return episodes.stream()
      .map(ep -> toWork(ep, serie))
      .collect(Collectors.toList());
  }

  private synchronized List<Work> findChildren(ProductionIdentifier identifier) {  // Only used for cases where there is no local serie
    Serie serie = (Serie)queryProduction(identifier);

    return serie.getSeasons().stream()
      .map(Season::getEpisodes)
      .flatMap(Collection::stream)
      .map(ep -> toWork(ep, serie))
      .collect(Collectors.toList());
  }

  public synchronized List<Contribution> findContributions(WorkId workId) {
    Identifier identifier = workId.getIdentifier();

    for(RolesQueryService rolesQueryService : rolesQueryServices) {
      if(rolesQueryService.getDataSourceName().equals(identifier.getDataSource().getName())) {
        return rolesQueryService.query(identifier).stream()
          .map(pr -> new Contribution(pr.getPerson(), pr.getRole(), pr.getOrder()))
          .collect(Collectors.toList());
      }
    }

    return Collections.emptyList();
  }

  public synchronized List<Work> findRecommendations(WorkId workId) {
    return recommendationQueryServices.get(0).query((ProductionIdentifier)workId.getIdentifier()).stream()
      .map(p -> toWork(p, null))
      .collect(Collectors.toList());
  }

  private Optional<ProductionCollection> queryProductionCollection(Identifier identifier) {
    MediaDescriptor mediaDescriptor = descriptorStore.find(identifier).orElse(null);

    if(mediaDescriptor instanceof IdentifierCollection) {  // If cached, get it from cache instead
      IdentifierCollection identifierCollection = (IdentifierCollection)mediaDescriptor;

      return Optional.of(new ProductionCollection(
        identifierCollection.getCollectionDetails(),
        identifierCollection.getItems().stream()
          .map(descriptorStore::find)
          .flatMap(Optional::stream)
          .filter(Production.class::isInstance)
          .map(Production.class::cast)
          .collect(Collectors.toList())
      ));
    }

    return Optional.ofNullable(productionCollectionQueryServices.get(0).query(identifier));
  }

  Work toWork(MediaDescriptor descriptor, MediaDescriptor parentDescriptor) {
    if(parentDescriptor != null) {
      List<BasicStream> childStreams = serieHelper.findChildStreams(parentDescriptor.getIdentifier(), descriptor.getIdentifier());

      /*
       * When multiple streams are returned for an Episode, there are two cases:
       * - Stream is split over multiple files
       * - There are multiple versions of the same episode (or one is a preview)
       *
       * The top level State should be a reduction of all the stream states,
       * but currently we just pick the first stream's State.
       */

      List<MediaStream> mediaStreams = childStreams.stream()
        .map(bs -> toMediaStream(bs, null))  // TODO Might want to provide Identification here based on parent
        .collect(Collectors.toList());

      return new Work(
        descriptor,
        new Parent(new WorkId(parentDescriptor.getIdentifier()), parentDescriptor.getIdentifier().getDataSource().getType(), parentDescriptor.getDetails().getName()),
        mediaStreams.stream().findFirst().map(MediaStream::getState).orElse(UNWATCHED_STATE),
        mediaStreams
      );
    }

    List<MediaStream> mediaStreams = streamStore.findStreams(descriptor.getIdentifier()).stream()
      .map(bs -> toMediaStream(bs, streamStore.findIdentifications(bs.getId()).getOrDefault(descriptor.getIdentifier(), null)))
      .collect(Collectors.toList());

    return new Work(
      descriptor,
      createParentForMovie(descriptor),
      mediaStreams.stream().findFirst().map(MediaStream::getState).orElse(UNWATCHED_STATE),
      mediaStreams
    );
  }

  private Parent createParentForMovie(MediaDescriptor descriptor) {
    Parent parent = null;

    if(descriptor instanceof Movie) {
      Movie movie = (Movie)descriptor;

      parent = movie.getCollectionIdentifier()
        .flatMap(ci -> find(new WorkId(ci)))
        .map(r -> new Parent(r.getId(), COLLECTION, r.getDetails().getName()))
        .orElse(null);
    }

    return parent;
  }

  private State toState(BasicStream bs) {
    StreamID streamId = bs.getId();

    if(bs.getType().equals(SERIE)) {
      Instant lastWatchedTime = null;
      int consumed = 0;

      for(BasicStream childStream : bs.getChildren()) {
        Instant lwt = stateService.getLastWatchedTime(childStream.getId());

        if(lastWatchedTime == null || (lwt != null && lwt.isAfter(lastWatchedTime))) {
          lastWatchedTime = lwt;
        }

        if(stateService.isWatched(childStream.getId())) {
          consumed++;
        }
      }

      return new State(lastWatchedTime, consumed >= bs.getChildren().size(), Duration.ZERO);
    }

    Instant lastWatchedTime = stateService.getLastWatchedTime(streamId);
    boolean watched = stateService.isWatched(streamId);
    Duration resumePosition = Duration.ofSeconds(stateService.getResumePosition(streamId));

    return new State(lastWatchedTime, watched, resumePosition);
  }

  private MediaStream toMediaStream(BasicStream bs, Identification identification) {
    StreamID streamId = bs.getId();
    StreamID parentId = streamStore.findParentId(streamId).orElse(null);
    State state = toState(bs);
    StreamMetaData md = metaDataStore.find(streamId).orElse(null);
    int totalDuration = stateService.getTotalDuration(streamId);

    if(md == null && totalDuration != -1) {
      md = new StreamMetaData(streamId, Duration.ofSeconds(totalDuration), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    return new MediaStream(
      streamId,
      parentId,
      new StreamAttributes(bs.getUri(), streamStore.findCreationTime(streamId).orElseThrow(), bs.getAttributes()),
      state,
      md,
      identification
    );
  }

  Work toWork(BasicStream bs) {
    StreamID streamId = bs.getId();
    Optional<Tuple2<MediaDescriptor, Identification>> tuple = findBestDescriptor(streamId);
    State state = toState(bs);

    List<MediaStream> mediaStreams = tuple.map(t -> t.a)
      .map(MediaDescriptor::getIdentifier)
      .map(streamStore::findStreamsAndIdentifications)  // won't work with an Episode identifier
      .map(Map::entrySet)
      .map(Collection::stream)
      .map(s -> s.map(e -> toMediaStream(e.getKey(), e.getValue())).collect(Collectors.toList()))
      .orElse(null);

    // TODO need to find a descriptor directly in DescriptorStore even for Episodes...
    if(mediaStreams == null || mediaStreams.isEmpty()) {
      mediaStreams = List.of(toMediaStream(bs, tuple.map(t -> t.b).orElse(null)));
    }

    MediaDescriptor descriptor = tuple.map(t -> t.a).orElseGet(() -> createMinimalDescriptor(bs));

    return new Work(
      descriptor,
      createParentForMovie(descriptor),
      state,
      mediaStreams
    );
  }

  private MediaDescriptor createMinimalDescriptor(BasicStream basicStream) {
    return new Production(
      new ProductionIdentifier(DataSource.instance(basicStream.getType(), "DEFAULT"), "" + basicStream.getId().asInt()),
      serieHelper.createMinimalDetails(basicStream),
      null,
      List.of(),
      List.of(),
      0.0,
      Set.of()
    );
  }

  private Optional<Tuple2<MediaDescriptor, Identification>> findBestDescriptor(StreamID streamId) {
    StreamID parentId = streamStore.findParentId(streamId).orElse(null);

    if(parentId != null) {
      return findBestIdentification(parentId)
        .flatMap(e -> identifierToEpisodeIdentification(e, streamStore.findStream(streamId).get().getAttributes()));
    }

    return findBestIdentification(streamId)
      .flatMap(this::identifierToProductioneIdentification);
  }

  private Optional<Entry<Identifier, Identification>> findBestIdentification(StreamID streamId) {
    return streamStore.findIdentifications(streamId).entrySet().stream()
      .sorted(DATA_SOURCE_PRIORITY)
      .findFirst();
  }

  private Optional<Tuple2<MediaDescriptor, Identification>> identifierToProductioneIdentification(Entry<Identifier, Identification> e) {
    return descriptorStore.find(e.getKey())
      .map(ep -> Tuple.of(ep, e.getValue()));
  }

  private Optional<Tuple2<MediaDescriptor, Identification>> identifierToEpisodeIdentification(Entry<Identifier, Identification> e, Attributes childAttributes) {
    return descriptorStore.find(e.getKey())
      .filter(Serie.class::isInstance)
      .map(Serie.class::cast)
      .map(s -> serieHelper.findChildDescriptors(s, childAttributes))
      .orElse(List.of())
      .stream()
      .findFirst()  // If multiple episodes are found, a reduce could be done (2 episodes matching a single stream)
      .map(ep -> Tuple.of(ep, e.getValue()));
  }
}

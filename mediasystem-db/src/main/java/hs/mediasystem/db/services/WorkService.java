package hs.mediasystem.db.services;

import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.db.base.StreamCacheUpdateService;
import hs.mediasystem.db.base.StreamStateService;
import hs.mediasystem.db.extract.DefaultStreamMetaDataStore;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.State;
import hs.mediasystem.domain.work.StreamAttributes;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.EpisodeIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.IdentifierCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.ProductionCollectionQueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.mediamanager.DescriptorStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkService {
  private static final String DEFAULT_DATA_SOURCE_NAME = "DEFAULT";
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType EPISODE = MediaType.of("EPISODE");
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
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private StreamCacheUpdateService updateService;
  @Inject private ContentPrintProvider contentPrintProvider;

  private static final State UNWATCHED_STATE = new State(null, false, Duration.ZERO);

  synchronized Optional<Work> find(StreamID streamId) {
    return streamStore.findStream(streamId).map(this::toWork);
  }

  /**
   * Finds the first match for a given {@link ContentID}.  Although rare, it is
   * possible that two streams have the same content.  As sometimes only the
   * content id is known (for example watched state is tracked by content id only)
   * this method does a best effort match.
   *
   * @param contentId a {@link ContentID}, cannot be null
   * @return an optional {@link Work}, never null
   */
  synchronized Optional<Work> findFirst(ContentID contentId) {
    return streamStore.findStreams(contentId).stream().findFirst().map(this::toWork);
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
    if(workId.getDataSource().getName().equals(DEFAULT_DATA_SOURCE_NAME)) {
      return find(StreamID.of(workId.getKey()));
    }
    if(workId.getType().equals(COLLECTION)) {
      return queryProductionCollection(toIdentifier(workId))
        .map(pc -> toWork(pc, null));
    }

    Identifier identifier = toIdentifier(workId);

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
    MediaType type = workId.getType();

    if(type.equals(SERIE)) {

      /*
       * Case 1: Identifier is a special identifier identifying a stream directly
       * Case 2: Identifier is found in StreamStore
       * Case 3: Identifier is has no stream associated with it whatsoever
       */

      if(workId.getDataSource().getName().equals(DEFAULT_DATA_SOURCE_NAME)) {
        return findChildren(StreamID.of(workId.getKey()));
      }

      List<Streamable> streamables = streamStore.findStreams(toIdentifier(workId));

      if(!streamables.isEmpty()) {
        return findChildren(streamables.get(0).getId());  // TODO if multiple Series have the same identifier, this picks one at random
      }

      return findChildren((ProductionIdentifier)toIdentifier(workId));
    }
    else if(type.equals(COLLECTION)) {
      return queryProductionCollection(toIdentifier(workId)).map(ProductionCollection::getItems).orElse(List.of()).stream()
        .map(p -> toWork(p, null))
        .collect(Collectors.toList());
    }

    return List.of();
  }

  // find children needs to always add all known children from the Serie, plus any that couldn't be matched
  // as extras!
  private synchronized List<Work> findChildren(StreamID id) {
    return streamStore.findStream(id)
      .map(this::findBestDescriptor)
      .filter(Serie.class::isInstance)
      .map(Serie.class::cast)
      .stream()  // could be 0 or 1, empty list results if 0, which basically means stream had no children
      .flatMap(serie ->
        Stream.concat(
          serie.getSeasons().stream().map(Season::getEpisodes).flatMap(Collection::stream),
          serieHelper.createExtras(serie, id).stream()
        )
        .map(ep -> toWork(ep, serie))
      )
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

  public synchronized List<VideoLink> findVideoLinks(WorkId workId) {
    return videoLinksQueryServices.get(0).query(toIdentifier(workId));
  }

  public synchronized List<Contribution> findContributions(WorkId workId) {
    Identifier identifier = toIdentifier(workId);

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
    return recommendationQueryServices.get(0).query((ProductionIdentifier)toIdentifier(workId)).stream()
      .map(p -> toWork(p, null))
      .collect(Collectors.toList());
  }

  public synchronized void reidentify(WorkId id) {
    find(id).stream()
      .map(Work::getStreams)
      .flatMap(Collection::stream)
      .map(MediaStream::getId)
      .forEach(updateService::reidentifyStream);
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
      Parent parent = new Parent(new WorkId(parentDescriptor.getIdentifier().getDataSource(), parentDescriptor.getIdentifier().getId()), parentDescriptor.getDetails().getTitle());
      Episode ep = (Episode)descriptor;
      Episode episode = new Episode(  // New Episode created here to add backdrop to Episode if one is missing
        (EpisodeIdentifier)ep.getIdentifier(),
        new Details(
          ep.getTitle(),
          ep.getDetails().getSubtitle().orElse(null),
          ep.getDescription().orElse(null),
          ep.getDate().orElse(null),
          ep.getImage().orElse(null),
          ep.getBackdrop().or(() -> parentDescriptor.getDetails().getBackdrop()).orElse(null)
        ),
        ep.getReception(),
        ep.getDuration(),
        ep.getSeasonNumber(),
        ep.getNumber(),
        ep.getPersonRoles()
      );

      List<MediaStream> mediaStreams;

      if(descriptor.getIdentifier().getDataSource().getName().equals(DEFAULT_DATA_SOURCE_NAME)) {
        String id = descriptor.getIdentifier().getId();
        int slash = id.indexOf("/");
        StreamID sid = StreamID.of(id.substring(slash + 1));

        mediaStreams = streamStore.findStream(sid).map(this::toMediaStream).stream().collect(Collectors.toList());
      }
      else {
        List<Streamable> childStreams = streamStore.findStreams(descriptor.getIdentifier());

        /*
         * When multiple streams are returned for an Episode, there are two cases:
         * - Stream is split over multiple files
         * - There are multiple versions of the same episode (or one is a preview)
         *
         * The top level State should be a reduction of all the stream states,
         * but currently we just pick the first stream's State.
         */

        mediaStreams = childStreams.stream()
          .map(this::toMediaStream)
          .collect(Collectors.toList());
      }

      return new Work(
        episode,
        parent,
        mediaStreams.stream().findFirst().map(MediaStream::getState).orElse(UNWATCHED_STATE),
        mediaStreams
      );
    }

    List<MediaStream> mediaStreams = streamStore.findStreams(descriptor.getIdentifier()).stream()
      .map(this::toMediaStream)
      .collect(Collectors.toList());

    return new Work(
      descriptor,
      createParentForMovieOrEpisode(descriptor),
      mediaStreams.stream().findFirst().map(MediaStream::getState).orElse(UNWATCHED_STATE),
      mediaStreams
    );
  }

  private Parent createParentForMovieOrEpisode(MediaDescriptor descriptor) {
    if(descriptor instanceof Movie) {
      Movie movie = (Movie)descriptor;

      return movie.getCollectionIdentifier()
        .flatMap(ci -> find(toWorkId(ci)))
        .map(r -> new Parent(r.getId(), r.getDetails().getTitle()))
        .orElse(null);
    }

    if(descriptor instanceof Episode) {
      return descriptorStore.find(descriptor.getIdentifier().getRootIdentifier())
        .map(d -> new Parent(toWorkId(d.getIdentifier()), d.getDetails().getTitle()))
        .orElse(null);
    }

    return null;
  }

  private State toState(Streamable streamable) {
    ContentID contentId = streamable.getId().getContentId();

    // TODO for Series, need to compute last watched time and watched status based on its children

    Instant lastWatchedTime = stateService.getLastWatchedTime(contentId);
    boolean watched = stateService.isWatched(contentId);
    Duration resumePosition = Duration.ofSeconds(stateService.getResumePosition(contentId));

    return new State(lastWatchedTime, watched, resumePosition);
  }

  private MediaStream toMediaStream(Streamable streamable) {
    Match match = streamStore.findIdentification(streamable.getId()).map(Identification::getMatch).orElse(null);
    StreamID id = streamable.getId();
    StreamID parentId = streamStore.findParentId(id).orElse(null);
    State state = toState(streamable);
    StreamMetaData md = metaDataStore.find(id.getContentId()).orElse(null);
    int totalDuration = stateService.getTotalDuration(id.getContentId());

    if(md == null && totalDuration != -1) {
      md = new StreamMetaData(id.getContentId(), Duration.ofSeconds(totalDuration), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    ContentPrint contentPrint = contentPrintProvider.get(id.getContentId());

    return new MediaStream(
      id,
      parentId,
      new StreamAttributes(streamable.getUri(), streamStore.findCreationTime(id).orElseThrow(), Instant.ofEpochMilli(contentPrint.getLastModificationTime()), contentPrint.getSize(), streamable.getAttributes()),
      state,
      md,
      match
    );
  }

  Work toWork(Streamable streamable) {
    MediaDescriptor descriptor = findBestDescriptor(streamable);
    State state = toState(streamable);

    List<MediaStream> mediaStreams = streamStore.findStreams(descriptor.getIdentifier()).stream()
      .map(this::toMediaStream)
      .collect(Collectors.toList());

    if(mediaStreams.isEmpty()) {
      mediaStreams = List.of(toMediaStream(streamable));
    }

    return new Work(
      descriptor,
      createParentForMovieOrEpisode(descriptor),
      state,
      mediaStreams
    );
  }

  private MediaDescriptor createMinimalDescriptor(Streamable streamable) {
    return new Production(
      new ProductionIdentifier(DataSource.instance(streamable.getType(), DEFAULT_DATA_SOURCE_NAME), "" + streamable.getId().asString()),
      serieHelper.createMinimalDetails(streamable),
      null,
      List.of(),
      List.of(),
      0.0,
      Set.of()
    );
  }

  private MediaDescriptor findBestDescriptor(Streamable streamable) {

    /*
     * Note: this uses the first identifier for a Stream (and thus the first descriptor).
     * A Stream can have multiple identifiers when a Stream spans multiple episodes,
     * but the Enrichment Data Source has them listed separately.
     */

    return streamStore.findIdentification(streamable.getId())
        .flatMap(identification -> descriptorStore.find(identification.getPrimaryIdentifier()))
        .orElseGet(() -> createMinimalDescriptor(streamable));
  }

  private static WorkId toWorkId(Identifier identifier) {
    return new WorkId(identifier.getDataSource(), identifier.getId());
  }

  private static Identifier toIdentifier(WorkId workId) {
    if(workId.getType().equals(EPISODE)) {
      return new EpisodeIdentifier(workId.getDataSource(), workId.getKey());
    }
    else if(workId.getType().equals(SERIE) || workId.getType().equals(MOVIE)) {
      return new ProductionIdentifier(workId.getDataSource(), workId.getKey());
    }

    return new Identifier(workId.getDataSource(), workId.getKey());
  }
}

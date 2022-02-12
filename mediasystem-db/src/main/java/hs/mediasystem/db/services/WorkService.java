package hs.mediasystem.db.services;

import hs.mediasystem.db.base.DatabaseStreamStore;
import hs.mediasystem.db.base.StreamCacheUpdateService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Classification;
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
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.ProductionCollectionQueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.util.Throwables;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

// TODO document why the public methods are synchronized
@Singleton
public class WorkService {

  /**
   * DataSource which uses as key a StreamID.<p>
   *
   * This internal data source is used for items that are part of a serie but have
   * not been matched up to a known episode or special.
   */
  static final String DEFAULT_DATA_SOURCE_NAME = "@INTERNAL";

  @Inject private DatabaseStreamStore streamStore;
  @Inject private DescriptorStore descriptorStore;
  @Inject private SerieHelper serieHelper;
  @Inject private List<QueryService> queryServices;
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<ProductionCollectionQueryService> productionCollectionQueryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private StreamCacheUpdateService updateService;
  @Inject private MediaStreamService mediaStreamService;

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
        return Throwables.uncheck(() -> (T)queryService.query(identifier));
      }
    }

    return null;
  }

  public synchronized Optional<Work> find(WorkId workId) {
    if(workId.getDataSource().getName().equals(DEFAULT_DATA_SOURCE_NAME)) {
      return find(StreamID.of(workId.getKey()));
    }
    if(workId.getType().equals(MediaType.COLLECTION)) {
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

    if(type.isSerie()) {

      /*
       * Case 1: Identifier is a special identifier identifying a stream directly
       * Case 2: Identifier is found in StreamStore
       * Case 3: Identifier is has no stream associated with it whatsoever
       */

      if(workId.getDataSource().getName().equals(DEFAULT_DATA_SOURCE_NAME)) {
        return findSerieChildren(StreamID.of(workId.getKey()));
      }

      List<Streamable> streamables = streamStore.findStreams(toIdentifier(workId));

      if(!streamables.isEmpty()) {
        return findSerieChildren(streamables.get(0).getId());  // TODO if multiple Series have the same identifier, this picks one at random
      }

      return findSerieChildren((ProductionIdentifier)toIdentifier(workId));
    }
    else if(type.equals(MediaType.COLLECTION)) {
      return queryProductionCollection(toIdentifier(workId)).map(ProductionCollection::getItems).orElse(List.of()).stream()
        .map(p -> toWork(p, null))
        .collect(Collectors.toList());
    }
    else if(type.equals(MediaType.FOLDER)) {
      return streamStore.findChildren(StreamID.of(workId.getKey())).stream()
        .map(this::toWork)
        .collect(Collectors.toList());
    }

    return List.of();
  }

  // find children needs to always add all known children from the Serie, plus any that couldn't be matched
  // as extras!
  private synchronized List<Work> findSerieChildren(StreamID id) {
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

  private synchronized List<Work> findSerieChildren(ProductionIdentifier identifier) {  // Only used for cases where there is no local serie
    Serie serie = (Serie)queryProduction(identifier);

    return serie.getSeasons().stream()
      .map(Season::getEpisodes)
      .flatMap(Collection::stream)
      .map(ep -> toWork(ep, serie))
      .collect(Collectors.toList());
  }

  public synchronized List<VideoLink> findVideoLinks(WorkId workId) {
    return Throwables.uncheck(() -> videoLinksQueryServices.get(0).query(toIdentifier(workId)));
  }

  public synchronized List<Contribution> findContributions(WorkId workId) {
    Identifier identifier = toIdentifier(workId);

    for(RolesQueryService rolesQueryService : rolesQueryServices) {
      if(rolesQueryService.getDataSourceName().equals(identifier.getDataSource().getName())) {
        return Throwables.uncheck(() -> rolesQueryService.query(identifier)).stream()
          .map(pr -> new Contribution(pr.getPerson(), pr.getRole(), pr.getOrder()))
          .collect(Collectors.toList());
      }
    }

    return Collections.emptyList();
  }

  public synchronized List<Work> findRecommendations(WorkId workId) {
    return Throwables.uncheck(() -> recommendationQueryServices.get(0).query((ProductionIdentifier)toIdentifier(workId))).stream()
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

    if(mediaDescriptor instanceof IdentifierCollection identifierCollection) {  // If cached, get it from cache instead
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

    return Optional.ofNullable(Throwables.uncheck(() -> productionCollectionQueryServices.get(0).query(identifier)));
  }

  // TODO This can potentially be extremely slow as it can do for example a collection query... not acceptable really
  Work toWork(Streamable streamable) {
    MediaDescriptor descriptor = findBestDescriptor(streamable);
    MediaDescriptor parentDescriptor = streamStore.findParentId(streamable.getId())
      .flatMap(streamStore::findStream)
      .map(this::findBestDescriptor)
      .orElse(null);

    return toWork(descriptor, parentDescriptor);
  }

  Work toWork(MediaDescriptor descriptor, MediaDescriptor parentDescriptor) {
    return new Work(
      descriptor,
      parentDescriptor == null ? createParentForMovieOrEpisode(descriptor).orElse(null) : createParent(parentDescriptor),
      createMediaStreams(descriptor.getIdentifier())
    );
  }

  private Optional<Parent> createParentForMovieOrEpisode(MediaDescriptor descriptor) {
    if(descriptor instanceof Movie movie) {
      return movie.getCollectionIdentifier()
        .flatMap(ci -> find(toWorkId(ci)))   // TODO this can trigger a remote look-up
        .map(w -> new Parent(w.getId(), w.getDetails().getTitle(), w.getDetails().getBackdrop().orElse(null)));
    }

    if(descriptor instanceof Episode) {
      return descriptorStore.find(descriptor.getIdentifier().getRootIdentifier())
        .map(this::createParent);
    }

    return Optional.empty();
  }

  private List<MediaStream> createMediaStreams(Identifier identifier) {
    List<MediaStream> mediaStreams;

    if(identifier.getDataSource().getName().equals(DEFAULT_DATA_SOURCE_NAME)) {
      String id = identifier.getId();
      StreamID sid = StreamID.of(id.substring(id.indexOf("/") + 1));

      mediaStreams = streamStore.findStream(sid).map(mediaStreamService::toMediaStream).stream().collect(Collectors.toList());
    }
    else {

      /*
       * When multiple streams are returned for an Episode, there are two cases:
       * - Stream is split over multiple files
       * - There are multiple versions of the same episode (or one is a preview)
       *
       * The top level State should be a reduction of all the stream states,
       * but currently we just pick the first stream's State.
       */

      mediaStreams = streamStore.findStreams(identifier).stream()
        .map(mediaStreamService::toMediaStream)
        .collect(Collectors.toList());
    }

    return mediaStreams;
  }

  private Parent createParent(MediaDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Parent(toWorkId(descriptor.getIdentifier()), details.getTitle(), details.getBackdrop().orElse(null));
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

  private MediaDescriptor createMinimalDescriptor(Streamable streamable) {
    return new Production(
      new ProductionIdentifier(DataSource.instance(streamable.getType(), DEFAULT_DATA_SOURCE_NAME), "" + streamable.getId().asString()),
      serieHelper.createMinimalDetails(streamable),
      null,
      Classification.EMPTY,
      0.0,
      Set.of()
    );
  }

  private static WorkId toWorkId(Identifier identifier) {
    return new WorkId(identifier.getDataSource(), identifier.getId());
  }

  private static Identifier toIdentifier(WorkId workId) {
    if(workId.getType().equals(MediaType.EPISODE)) {
      return new EpisodeIdentifier(workId.getDataSource(), workId.getKey());
    }
    else if(workId.getType().equals(MediaType.SERIE) || workId.getType().equals(MediaType.MOVIE)) {
      return new ProductionIdentifier(workId.getDataSource(), workId.getKey());
    }

    return new Identifier(workId.getDataSource(), workId.getKey());
  }
}

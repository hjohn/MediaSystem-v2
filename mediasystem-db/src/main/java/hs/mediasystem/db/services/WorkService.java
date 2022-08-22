package hs.mediasystem.db.services;

import hs.mediasystem.db.base.StreamCacheUpdateService;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.WorkIdCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.ProductionCollectionQueryService;
import hs.mediasystem.ext.basicmediatypes.services.QueryService;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.mediamanager.DescriptorStore;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.checked.CheckedStreams;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkService {
  @Inject private LinkedWorksService linkedWorksService;
  @Inject private DescriptorStore descriptorStore;
  @Inject private List<QueryService> queryServices;
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<ProductionCollectionQueryService> productionCollectionQueryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private StreamCacheUpdateService updateService;
  @Inject private MediaStreamService mediaStreamService;

  public Optional<Work> query(WorkId workId) throws IOException {
    Optional<Work> optionalWork = linkedWorksService.find(workId).map(this::toWork);

    if(optionalWork.isPresent()) {
      return optionalWork;
    }

    if(workId.getType().equals(MediaType.COLLECTION)) {
      return queryProductionCollection(workId)
        .map(this::toWork)
        .toOptional();
    }

    return queryEpisode(workId)
      .or(() -> CheckedOptional.from(descriptorStore.find(workId))
        .or(() -> queryDescriptor(workId))
        .map(this::toWork)
      )
      .toOptional();
  }

  public List<Work> queryChildren(WorkId workId) throws IOException {
    MediaType type = workId.getType();

    if(type.isSerie()) {
      LinkedWork parent = linkedWorksService.find(workId).orElse(null);

      if(parent != null) {
        List<LinkedWork> children = linkedWorksService.findChildren(workId);

        if(!children.isEmpty()) {
          return toMixedWorks(parent, children);
        }
      }

      return querySerieChildren(workId);
    }
    else if(type.equals(MediaType.COLLECTION)) {
      return CheckedStreams.forIOException(queryProductionCollection(workId).map(ProductionCollection::getItems).orElse(List.of()))
        .map(this::toWork)
        .collect(Collectors.toList());
    }
    else if(type.equals(MediaType.FOLDER)) {
      return linkedWorksService.findChildren(workId).stream().map(this::toWork).toList();
    }

    return List.of();
  }

  public List<VideoLink> queryVideoLinks(WorkId workId) {
    return Throwables.uncheck(() -> videoLinksQueryServices.get(0).query(workId));
  }

  public List<Contribution> queryContributions(WorkId workId) {
    for(RolesQueryService rolesQueryService : rolesQueryServices) {
      if(rolesQueryService.getDataSourceName().equals(workId.getDataSource().getName())) {
        return Throwables.uncheck(() -> rolesQueryService.query(workId)).stream()
          .map(pr -> new Contribution(pr.getPerson(), pr.getRole(), pr.getOrder()))
          .collect(Collectors.toList());
      }
    }

    return Collections.emptyList();
  }

  public List<Work> queryRecommendations(WorkId workId) throws IOException {
    return CheckedStreams.forIOException(recommendationQueryServices.get(0).query(workId))
      .map(this::toWork)
      .collect(Collectors.toList());
  }

  public void reidentify(WorkId id) throws IOException {
    query(id).stream()
      .map(Work::getStreams)
      .flatMap(Collection::stream)
      .map(MediaStream::id)
      .forEach(updateService::reidentifyStream);
  }

  Optional<Work> findFirst(StreamID streamId) {
    return linkedWorksService.find(streamId).stream().map(this::toWork).findFirst();
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
  Optional<Work> findFirst(ContentID contentId) {
    return linkedWorksService.find(contentId).stream().map(this::toWork).findFirst();
  }

  Work toWork(LinkedWork linkedWork) {
    return new Work(
      linkedWork.work().descriptor(),
      linkedWork.work().parent().map(p -> new Parent(p.id(), p.title(), p.backdrop())).orElse(null),
      linkedWork.matchedResources().stream().map(mediaStreamService::toMediaStream).toList()
    );
  }

  Work toWork(WorkDescriptor descriptor) throws IOException {
    return CheckedOptional.from(linkedWorksService.find(descriptor.getId()))
      .map(this::toWork)
      .orElseGet(() -> toRemoteWork(descriptor, null));
  }

  private List<Work> toMixedWorks(LinkedWork parent, List<LinkedWork> children) throws IOException {  // contains works with and without resources
    Map<WorkId, Work> episodesWithStreams = new HashMap<>();

    for(LinkedWork child : children) {
      episodesWithStreams.put(child.work().descriptor().getId(), toWork(child));
    }

    return Stream.concat(
      children.stream().map(this::toWork),
      CheckedOptional.of(parent.work().descriptor())
        .filter(Serie.class::isInstance)
        .map(Serie.class::cast)
        .stream()  // could be 0 or 1, empty list results if 0, which basically means stream had no children
        .declaring(IOException.class)
        .flatMap(serie ->
          CheckedStreams.forIOException(serie.getSeasons())
            .map(Season::getEpisodes)
            .flatMap(CheckedStreams::forIOException)
            .filter(ep -> !episodesWithStreams.containsKey(ep.getId()))
            .map(ep -> toRemoteWork(ep, serie))
        )
        .toList()
        .stream()
    ).toList();
  }

  private List<Work> querySerieChildren(WorkId id) throws IOException {  // Only used for cases where there is no local serie
    return queryDescriptor(id)
      .filter(Serie.class::isInstance)
      .map(Serie.class::cast)
      .map(serie -> CheckedStreams.forIOException(serie.getSeasons())
        .map(Season::getEpisodes)
        .flatMap(CheckedStreams::forIOException)
        .map(ep -> toRemoteWork(ep, serie))
        .collect(Collectors.toList())
      )
      .orElse(List.of());
  }

  private CheckedOptional<Work> queryEpisode(WorkId id) throws IOException {
    WorkId parentId = id.getParent().orElse(null);

    if(parentId == null) {
      return CheckedOptional.empty();
    }

    return CheckedOptional.from(descriptorStore.find(parentId))
      .or(() -> queryDescriptor(parentId))
      .map(Serie.class::cast)
      .flatMap(serie -> CheckedStreams.forIOException(serie.getSeasons())
        .map(Season::getEpisodes)
        .flatMap(CheckedStreams::forIOException)
        .filter(ep -> ep.getId().equals(id))
        .findFirst()
        .map(e -> toRemoteWork(e, serie))
      );
  }

  private CheckedOptional<WorkDescriptor> queryDescriptor(WorkId id) throws IOException {
    for(QueryService queryService : queryServices) {
      if(queryService.getDataSource().equals(id.getDataSource())) {
        return CheckedOptional.of(queryService.query(id));
      }
    }

    return CheckedOptional.empty();
  }

  private CheckedOptional<ProductionCollection> queryProductionCollection(WorkId id) {
    WorkDescriptor workDescriptor = descriptorStore.find(id).orElse(null);

    if(workDescriptor instanceof WorkIdCollection workIdCollection) {  // If cached, get it from cache instead
      return CheckedOptional.of(new ProductionCollection(
        workIdCollection.getCollectionDetails(),
        workIdCollection.getItems().stream()
          .map(descriptorStore::find)
          .flatMap(Optional::stream)
          .filter(Production.class::isInstance)
          .map(Production.class::cast)
          .collect(Collectors.toList())
      ));
    }

    return CheckedOptional.ofNullable(Throwables.uncheck(() -> productionCollectionQueryServices.get(0).query(id)));
  }

  private Work toRemoteWork(WorkDescriptor descriptor, WorkDescriptor parentDescriptor) throws IOException {
    return new Work(
      descriptor,
      parentDescriptor == null ? queryParent(descriptor).orElse(null) : createParent(parentDescriptor),
      List.of()
    );
  }

  private CheckedOptional<Parent> queryParent(WorkDescriptor descriptor) throws IOException {
    if(descriptor instanceof Movie movie) {
      return CheckedOptional.from(movie.getCollectionId())
        .flatMap(ci -> CheckedOptional.from(query(ci)))   // this can trigger a remote look-up
        .map(Work::getDescriptor)
        .map(this::createParent);
    }

    if(descriptor instanceof Episode) {
      return CheckedOptional.from(descriptor.getId().getParent().flatMap(descriptorStore::find))
        .map(this::createParent);
    }

    return CheckedOptional.empty();
  }

  private Parent createParent(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Parent(descriptor.getId(), details.getTitle(), details.getBackdrop());
  }
}

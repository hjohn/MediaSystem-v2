package hs.mediasystem.db.services;

import hs.mediasystem.db.core.DescriptorService;
import hs.mediasystem.db.core.IdentifierService;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.Details;
import hs.mediasystem.ext.basicmediatypes.domain.Episode;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionCollection;
import hs.mediasystem.ext.basicmediatypes.domain.Season;
import hs.mediasystem.ext.basicmediatypes.domain.Serie;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.checked.CheckedStreams;

import java.io.IOException;
import java.net.URI;
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
  @Inject private LinkedResourcesService linkedResourcesService;
  @Inject private DescriptorService descriptorService;
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private MediaStreamService mediaStreamService;
  @Inject private IdentifierService identifierService;

  public Optional<Work> query(WorkId workId) throws IOException {
    Optional<Work> optionalWork = CheckedOptional.from(linkedWorksService.find(workId)).map(this::toWork).toOptional();

    if(optionalWork.isPresent()) {
      return optionalWork;
    }

    if(workId.getType().equals(MediaType.COLLECTION)) {
      return queryProductionCollection(workId)
        .map(this::toWork)
        .toOptional();
    }

    return queryEpisode(workId)
      .or(() -> CheckedOptional.from(descriptorService.find(workId))
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

    if(type.equals(MediaType.COLLECTION)) {
      return CheckedStreams.forIOException(queryProductionCollection(workId).map(ProductionCollection::getItems).orElse(List.of()))
        .map(this::toWork)
        .collect(Collectors.toList());
    }

    if(type.equals(MediaType.FOLDER)) {
      return CheckedStreams.forIOException(linkedWorksService.findChildren(workId)).map(this::toWork).toList();
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
      .flatMap(List::stream)
      .map(MediaStream::location)
      .forEach(identifierService::reidentify);
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
      CheckedStreams.forIOException(children).map(this::toWork).toList().stream(),
      CheckedOptional.of(parent.work().descriptor())
        .filter(Serie.class::isInstance)
        .map(Serie.class::cast)
        .stream()  // could be 0 or 1, empty list results if 0, which basically means stream had no children
        .declaring(IOException.class)
        .flatMap(serie ->
          CheckedStreams.forIOException(serie.getSeasons())
            .map(Season::getEpisodes)
            .flatMapStream(List::stream)
            .filter(ep -> !episodesWithStreams.containsKey(ep.getId()))
            .map(ep -> toRemoteWork(ep, serie))
        )
        .toList()
        .stream()
    ).toList();
  }

  private Work toWork(LinkedWork linkedWork) throws IOException {
    return new Work(
      linkedWork.work().descriptor(),
      findOrCreateParent(linkedWork).orElse(null),
      linkedWork.matchedResources().stream().map(mediaStreamService::toMediaStream).toList()
    );
  }

  private List<Work> querySerieChildren(WorkId id) throws IOException {  // Only used for cases where there is no local serie
    return CheckedOptional.from(descriptorService.find(id))
      .filter(Serie.class::isInstance)
      .map(Serie.class::cast)
      .map(serie -> CheckedStreams.forIOException(serie.getSeasons())
        .map(Season::getEpisodes)
        .flatMapStream(List::stream)
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

    return CheckedOptional.from(descriptorService.find(parentId))
      .map(Serie.class::cast)
      .flatMap(serie -> CheckedStreams.forIOException(serie.getSeasons())
        .map(Season::getEpisodes)
        .flatMapStream(List::stream)
        .filter(ep -> ep.getId().equals(id))
        .findFirst()
        .map(e -> toRemoteWork(e, serie))
      );
  }

  private Work toRemoteWork(WorkDescriptor descriptor, WorkDescriptor parentDescriptor) throws IOException {
    return new Work(
      descriptor,
      parentDescriptor == null ? queryParent(descriptor).orElse(null) : createParent(parentDescriptor),
      List.of()
    );
  }

  private CheckedOptional<Parent> findOrCreateParent(LinkedWork linkedWork) throws IOException {

    /*
     * A LinkedWork is always a local resource, so its parent information is based on that.
     * It can however be a Movie, in which case we should find the collection.
     */

    Resource resource = linkedWork.matchedResources().get(0).resource();

    return CheckedOptional.from(resource.parentLocation())
      .flatMapOpt(this::createParent)
      .or(() -> findParentForLocal(linkedWork.work().descriptor()));
  }

  private CheckedOptional<Parent> findParentForLocal(WorkDescriptor descriptor) throws IOException {
    // Only for collections, everything else should already be available for a local work
    if(descriptor instanceof Movie movie) {
      return queryParent(descriptor);
    }

    return CheckedOptional.empty();
  }

  private CheckedOptional<Parent> queryParent(WorkDescriptor descriptor) throws IOException {
    if(descriptor instanceof Movie movie) {
      return CheckedOptional.from(movie.getCollectionId())
        .flatMap(this::queryProductionCollection)
        .map(this::createParent);
    }

    if(descriptor instanceof Episode) {
      return CheckedOptional.from(descriptor.getId().getParent()).flatMapOpt(descriptorService::find)
        .map(this::createParent);
    }

    return CheckedOptional.empty();
  }

  private CheckedOptional<ProductionCollection> queryProductionCollection(WorkId id) throws IOException {
    return CheckedOptional.from(descriptorService.find(id))
      .filter(ProductionCollection.class::isInstance)
      .map(ProductionCollection.class::cast);
  }

  private Optional<Parent> createParent(URI parentUri) {
    return linkedResourcesService.find(parentUri)
      .map(lr -> createParent(lr.works().get(0).descriptor()));
  }

  private Parent createParent(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Parent(descriptor.getId(), details.getTitle(), details.getBackdrop());
  }
}

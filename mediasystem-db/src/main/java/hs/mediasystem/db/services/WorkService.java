package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.domain.Details;
import hs.mediasystem.api.datasource.domain.Episode;
import hs.mediasystem.api.datasource.domain.ProductionCollection;
import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.datasource.domain.Serie;
import hs.mediasystem.api.datasource.domain.stream.Contribution;
import hs.mediasystem.api.datasource.domain.stream.Work;
import hs.mediasystem.api.datasource.services.RecommendationQueryService;
import hs.mediasystem.api.datasource.services.RolesQueryService;
import hs.mediasystem.api.datasource.services.VideoLinksQueryService;
import hs.mediasystem.db.core.DescriptorService;
import hs.mediasystem.db.services.domain.LinkedWork;
import hs.mediasystem.db.services.domain.Resource;
import hs.mediasystem.domain.media.MediaStream;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.Context;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.checked.CheckedStreams;
import hs.mediasystem.util.exception.Throwables;

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
  @Inject private ResourceService resourceService;
  @Inject private DescriptorService descriptorService;
  @Inject private List<RolesQueryService> rolesQueryServices;
  @Inject private List<RecommendationQueryService> recommendationQueryServices;
  @Inject private List<VideoLinksQueryService> videoLinksQueryServices;
  @Inject private MediaStreamService mediaStreamService;

  public Optional<Work> query(WorkId workId) throws IOException {
    return CheckedOptional.from(linkedWorksService.find(workId))
      .map(this::toWork)
      .or(() -> CheckedOptional.from(descriptorService.find(workId).map(this::toWork)))
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
      return CheckedStreams.forIOException(descriptorService.find(workId).map(ProductionCollection.class::cast).map(ProductionCollection::getItems).orElse(List.of()))
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
      .forEach(resourceService::reidentify);
  }

  Work toWork(WorkDescriptor descriptor) {
    return CheckedOptional.from(linkedWorksService.find(descriptor.getId()))
      .map(this::toWork)
      .orElseGet(() -> toRemoteWork(descriptor, null));
  }

  private List<Work> toMixedWorks(LinkedWork parent, List<LinkedWork> children) throws IOException {  // contains works with and without resources
    Map<WorkId, Work> episodesWithStreams = new HashMap<>();

    for(LinkedWork child : children) {
      episodesWithStreams.put(child.workDescriptor().getId(), toWork(child));
    }

    return Stream.concat(
      CheckedStreams.forIOException(children).map(this::toWork).toList().stream(),
      CheckedOptional.of(parent.workDescriptor())
        .filter(Serie.class::isInstance)
        .map(Serie.class::cast)
        .stream()  // could be 0 or 1, empty list results if 0, which basically means stream had no children
        .declaring(IOException.class)
        .flatMap(serie ->
          CheckedStreams.forIOException(serie.getSeasons())
            .map(Serie.Season::episodes)
            .flatMapStream(List::stream)
            .filter(ep -> !episodesWithStreams.containsKey(ep.id()))
            .map(e -> toEpisode(serie, e))
            .map(ep -> toRemoteWork(ep, serie))
        )
        .toList()
        .stream()
    ).toList();
  }

  private Work toWork(LinkedWork linkedWork) {
    return new Work(
      linkedWork.workDescriptor(),
      findOrCreateContext(linkedWork).orElse(null),
      linkedWork.resources().stream().map(mediaStreamService::toMediaStream).toList()
    );
  }

  private List<Work> querySerieChildren(WorkId id) throws IOException {  // Only used for cases where there is no local serie
    return CheckedOptional.from(descriptorService.find(id))
      .filter(Serie.class::isInstance)
      .map(Serie.class::cast)
      .map(serie -> CheckedStreams.forIOException(serie.getSeasons())
        .map(Serie.Season::episodes)
        .flatMapStream(List::stream)
        .map(e -> toEpisode(serie, e))
        .map(ep -> toRemoteWork(ep, serie))
        .collect(Collectors.toList())
      )
      .orElse(List.of());
  }

  private static Work toRemoteWork(WorkDescriptor descriptor, WorkDescriptor parentDescriptor) {
    return new Work(
      descriptor,
      parentDescriptor == null ? queryContext(descriptor).orElse(null) : createContext(parentDescriptor),
      List.of()
    );
  }

  private Optional<Context> findOrCreateContext(LinkedWork linkedWork) {
    Resource resource = linkedWork.resources().getFirst();

    return linkedWork.workDescriptor() instanceof Release release
      ? release.getContext().or(() -> resource.streamable().parentLocation().flatMap(this::createContext))
      : Optional.empty();
  }

  private static Optional<Context> queryContext(WorkDescriptor descriptor) {
    if(descriptor instanceof Release release) {
      return release.getContext();
    }

    return Optional.empty();
  }

  private Optional<Context> createContext(URI parentUri) {
    return resourceService.find(parentUri)
      .map(lr -> createContext(lr.releases().getFirst()));
  }

  private static Context createContext(WorkDescriptor descriptor) {
    Details details = descriptor.getDetails();

    return new Context(descriptor.getId(), details.getTitle(), details.getCover(), details.getBackdrop());
  }

  private static Episode toEpisode(Serie serie, Serie.Episode episode) {
    return new Episode(
      episode.id(),
      episode.details(),
      episode.reception(),
      new Context(serie.getId(), serie.getTitle(), serie.getCover(), serie.getBackdrop()),
      episode.duration(),
      episode.seasonNumber(),
      episode.number(),
      episode.personRoles()
    );
  }
}

package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.Throwables;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalWorkClient implements WorkClient {
  @Inject private WorkService workService;
  @Inject private LocalWorksClient worksClient;
  @Inject private LocalPersonClient personClient;

  @Override
  public List<Work> findChildren(WorkId workId) {
    return Throwables.uncheck(() -> workService.queryChildren(workId)).stream().map(worksClient::toWork).collect(Collectors.toList());
  }

  @Override
  public Optional<Work> find(WorkId workId) {
    return Throwables.uncheck(() -> workService.query(workId)).map(worksClient::toWork);
  }

  @Override
  public List<Work> findRecommendations(WorkId workId) {
    return Throwables.uncheck(() -> workService.queryRecommendations(workId)).stream().map(worksClient::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Contribution> findContributions(WorkId id) {
    return workService.queryContributions(id).stream().map(this::toContribution).collect(Collectors.toList());
  }

  @Override
  public List<VideoLink> findVideoLinks(WorkId id) {
    return workService.queryVideoLinks(id);
  }

  @Override
  public void reidentify(WorkId id) {
    Throwables.uncheck(() -> workService.reidentify(id));
  }

  private Contribution toContribution(hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution c) {
    return new Contribution(
      personClient.toPerson(c.getPerson()),
      LocalPersonClient.toRole(c.getRole()),
      c.getOrder()
    );
  }
}

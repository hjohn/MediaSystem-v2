package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.domain.work.VideoLink;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ui.api.WorkClient;
import hs.mediasystem.ui.api.domain.Contribution;
import hs.mediasystem.ui.api.domain.Work;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalWorkClient implements WorkClient {
  @Inject private WorkService workService;
  @Inject private LocalWorksClient worksClient;

  @Override
  public List<Work> findChildren(WorkId workId) {
    return workService.findChildren(workId).stream().map(worksClient::toWork).collect(Collectors.toList());
  }

  @Override
  public Optional<Work> find(WorkId workId) {
    return workService.find(workId).map(worksClient::toWork);
  }

  @Override
  public List<Work> findRecommendations(WorkId workId) {
    return workService.findRecommendations(workId).stream().map(worksClient::toWork).collect(Collectors.toList());
  }

  @Override
  public List<Contribution> findContributions(WorkId id) {
    return workService.findContributions(id).stream().map(LocalWorkClient::toContribution).collect(Collectors.toList());
  }

  @Override
  public List<VideoLink> findVideoLinks(WorkId id) {
    return workService.findVideoLinks(id);
  }

  @Override
  public void reidentify(WorkId id) {
    workService.reidentify(id);
  }

  private static Contribution toContribution(hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution c) {
    return new Contribution(
      LocalPersonClient.toPerson(c.getPerson()),
      LocalPersonClient.toRole(c.getRole()),
      c.getOrder()
    );
  }
}

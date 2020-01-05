package hs.mediasystem.client;

import hs.mediasystem.db.services.WorkService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.PersonId;
import hs.mediasystem.ext.basicmediatypes.domain.stream.WorkId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkClient {
  @Inject private WorkService workService;
  @Inject private WorksClient worksClient;

  public List<Work> findChildren(WorkId workId) {
    return workService.findChildren(workId).stream().map(worksClient::toWork).collect(Collectors.toList());
  }

  public Optional<Work> find(WorkId workId) {
    return workService.find(workId).map(worksClient::toWork);
  }

  public List<Work> findRecommendations(WorkId workId) {
    return workService.findRecommendations(workId).stream().map(worksClient::toWork).collect(Collectors.toList());
  }

  public List<Contribution> findContributions(WorkId id) {
    return workService.findContributions(id).stream().map(WorkClient::toContribution).collect(Collectors.toList());
  }

  private static Contribution toContribution(hs.mediasystem.ext.basicmediatypes.domain.stream.Contribution c) {
    return new Contribution(
      toPerson(c.getPerson()),
      c.getRole(),
      c.getOrder()
    );
  }

  private static Person toPerson(hs.mediasystem.ext.basicmediatypes.domain.Person p) {
    return new Person(
      new PersonId(p.getIdentifier()),
      p.getName(),
      null,
      p.getImage(),
      null,
      0,
      null,
      null,
      null,
      List.of()
    );
  }
}

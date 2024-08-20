package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.domain.PersonalProfile;
import hs.mediasystem.api.datasource.domain.ProductionRole;
import hs.mediasystem.api.datasource.domain.stream.Participation;
import hs.mediasystem.api.datasource.domain.stream.Person;
import hs.mediasystem.api.datasource.domain.stream.Person.Gender;
import hs.mediasystem.api.datasource.domain.stream.Work;
import hs.mediasystem.api.datasource.services.PersonalProfileQueryService;
import hs.mediasystem.domain.work.PersonId;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersonService {
  @Inject private WorkService workService;
  @Inject private List<PersonalProfileQueryService> personalProfileQueryServices;

  public Optional<Person> findPerson(PersonId id) throws IOException {
    return personalProfileQueryServices.get(0).query(id).map(this::toPerson);
  }

  private Person toPerson(PersonalProfile pp) {
    return new Person(
      pp.getPerson().getId(),
      pp.getPerson().getName(),
      pp.getBiography(),
      pp.getPerson().getCover(),
      pp.getGender() == null ? null : Gender.valueOf(pp.getGender().toString()),
      pp.getPopularity(),
      pp.getBirthPlace(),
      pp.getBirthDate(),
      pp.getDeathDate(),
      pp.getProductionRoles().stream().map(this::toParticipation).collect(Collectors.toList())
    );
  }

  private Participation toParticipation(ProductionRole pr) {
    Work work = workService.toWork(pr.getProduction());

    return new Participation(
      pr.getRole(),
      work,
      pr.getEpisodeCount() == null ? 0 : pr.getEpisodeCount(),
      pr.getPopularity()
    );
  }
}

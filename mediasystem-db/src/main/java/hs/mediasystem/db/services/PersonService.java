package hs.mediasystem.db.services;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Participation;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Person;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Person.Gender;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.PersonalProfileQueryService;
import hs.mediasystem.util.checked.CheckedOptional;
import hs.mediasystem.util.checked.CheckedStreams;

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
    PersonalProfile personalProfile = personalProfileQueryServices.get(0).query(id);

    return CheckedOptional.ofNullable(personalProfile)
      .map(this::toPerson)
      .toOptional();
  }

  private Person toPerson(PersonalProfile pp) throws IOException {
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
      CheckedStreams.forIOException(pp.getProductionRoles()).map(this::toParticipation).collect(Collectors.toList())
    );
  }

  private Participation toParticipation(ProductionRole pr) throws IOException {
    Work work = workService.toWork(pr.getProduction());

    return new Participation(
      pr.getRole(),
      work,
      pr.getEpisodeCount() == null ? 0 : pr.getEpisodeCount(),
      pr.getPopularity()
    );
  }
}

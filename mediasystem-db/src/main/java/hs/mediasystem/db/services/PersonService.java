package hs.mediasystem.db.services;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Participation;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Person;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Person.Gender;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Work;
import hs.mediasystem.ext.basicmediatypes.services.PersonalProfileQueryService;
import hs.mediasystem.util.Throwables;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersonService {
  @Inject private WorkService workService;
  @Inject private List<PersonalProfileQueryService> personalProfileQueryServices;

  public Optional<Person> findPerson(PersonId id) {
    PersonalProfile personalProfile = Throwables.uncheck(() -> personalProfileQueryServices.get(0).query(new PersonIdentifier(id.getDataSource(), id.getKey())));

    return Optional.ofNullable(personalProfile)
      .map(this::toPerson);
  }

  private Person toPerson(PersonalProfile pp) {
    return new Person(
      new PersonId(pp.getPerson().getIdentifier().getDataSource(), pp.getPerson().getIdentifier().getId()),
      pp.getPerson().getName(),
      pp.getBiography(),
      pp.getPerson().getImage(),
      pp.getGender() == null ? null : Gender.valueOf(pp.getGender().toString()),
      pp.getPopularity(),
      pp.getBirthPlace(),
      pp.getBirthDate(),
      pp.getDeathDate(),
      pp.getProductionRoles().stream().map(this::toParticipation).collect(Collectors.toList())
    );
  }

  private Participation toParticipation(ProductionRole pr) {
    Work work = workService.toWork(pr.getProduction(), null);

    return new Participation(
      pr.getRole(),
      work,
      pr.getEpisodeCount() == null ? 0 : pr.getEpisodeCount(),
      pr.getPopularity()
    );
  }
}

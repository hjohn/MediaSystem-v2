package hs.mediasystem.client;

import hs.mediasystem.db.services.PersonService;
import hs.mediasystem.ext.basicmediatypes.domain.stream.PersonId;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PersonClient {
  @Inject private PersonService personService;
  @Inject private WorksClient worksClient;

  public Optional<Person> findPerson(PersonId id) {
    return personService.findPerson(id).map(this::toPerson);
  }

  private Person toPerson(hs.mediasystem.ext.basicmediatypes.domain.stream.Person p) {
    return new Person(
      p.getId(),
      p.getName(),
      p.getBiography().orElse(null),
      p.getImage().orElse(null),
      p.getGender().map(Object::toString).map(Person.Gender::valueOf).orElse(null),
      p.getPopularity(),
      p.getBirthPlace().orElse(null),
      p.getBirthDate().orElse(null),
      p.getDeathDate().orElse(null),
      p.getParticipations().stream().map(this::toParticipation).collect(Collectors.toList())
    );
  }

  private Participation toParticipation(hs.mediasystem.ext.basicmediatypes.domain.stream.Participation p) {
    return new Participation(
      p.getRole(),
      worksClient.toWork(p.getWork()),
      p.getEpisodeCount(),
      p.getPopularity()
    );
  }
}

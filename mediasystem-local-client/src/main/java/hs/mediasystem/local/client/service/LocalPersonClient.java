package hs.mediasystem.local.client.service;

import hs.mediasystem.db.services.PersonService;
import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.ui.api.PersonClient;
import hs.mediasystem.ui.api.domain.Participation;
import hs.mediasystem.ui.api.domain.Person;
import hs.mediasystem.ui.api.domain.Role;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.image.ImageHandleFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalPersonClient implements PersonClient {
  @Inject private PersonService personService;
  @Inject private LocalWorksClient worksClient;
  @Inject private ImageHandleFactory imageHandleFactory;

  @Override
  public Optional<Person> findPerson(PersonId id) {
    return Throwables.uncheck(() -> personService.findPerson(id)).map(this::toPerson);
  }

  private Person toPerson(hs.mediasystem.ext.basicmediatypes.domain.stream.Person p) {
    return new Person(
      p.getId(),
      p.getName(),
      p.getBiography().orElse(null),
      p.getCover().map(imageHandleFactory::fromURI).orElse(null),
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
      toRole(p.getRole()),
      worksClient.toWork(p.getWork()),
      p.getEpisodeCount(),
      p.getPopularity()
    );
  }

  Person toPerson(hs.mediasystem.ext.basicmediatypes.domain.Person p) {
    return new Person(
      p.getId(),
      p.getName(),
      null,
      p.getCover() == null ? null : imageHandleFactory.fromURI(p.getCover()),
      null,
      0,
      null,
      null,
      null,
      List.of()
    );
  }

  static Role toRole(hs.mediasystem.ext.basicmediatypes.domain.Role r) {
    return new Role(
      Role.Type.valueOf(r.getType().toString()),
      r.getCharacter(),
      r.getJob()
    );
  }
}

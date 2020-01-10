package hs.mediasystem.ui.api;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.ui.api.domain.Person;

import java.util.Optional;

public interface PersonClient {
  Optional<Person> findPerson(PersonId id);
}

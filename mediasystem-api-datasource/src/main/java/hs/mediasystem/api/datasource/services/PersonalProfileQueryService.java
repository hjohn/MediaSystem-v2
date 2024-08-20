package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.domain.PersonalProfile;
import hs.mediasystem.domain.work.PersonId;

import java.io.IOException;
import java.util.Optional;

public interface PersonalProfileQueryService {
  Optional<PersonalProfile> query(PersonId id) throws IOException;
}

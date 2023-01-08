package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.domain.PersonalProfile;
import hs.mediasystem.domain.work.PersonId;

import java.io.IOException;

public interface PersonalProfileQueryService {
  PersonalProfile query(PersonId id) throws IOException;
}

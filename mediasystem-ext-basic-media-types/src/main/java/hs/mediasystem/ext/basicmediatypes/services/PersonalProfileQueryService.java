package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;

import java.io.IOException;

public interface PersonalProfileQueryService {
  PersonalProfile query(PersonId id) throws IOException;
}

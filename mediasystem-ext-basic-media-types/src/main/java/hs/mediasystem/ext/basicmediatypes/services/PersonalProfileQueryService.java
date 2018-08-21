package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;

public interface PersonalProfileQueryService {
  PersonalProfile query(PersonIdentifier identifier);
}

package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonalProfile;

import java.io.IOException;

public interface PersonalProfileQueryService {
  PersonalProfile query(PersonIdentifier identifier) throws IOException;
}

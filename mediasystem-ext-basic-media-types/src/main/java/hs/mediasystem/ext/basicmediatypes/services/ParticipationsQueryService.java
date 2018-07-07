package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.domain.PersonIdentifier;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionRole;

import java.util.List;

public interface ParticipationsQueryService {
  List<ProductionRole> query(PersonIdentifier identifier);
}

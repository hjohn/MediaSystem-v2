package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;

import java.util.List;

public interface RolesQueryService {
  List<PersonRole> query(Identifier identifier);
}

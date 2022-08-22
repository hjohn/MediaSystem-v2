package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;

import java.io.IOException;
import java.util.List;

public interface RolesQueryService {
  String getDataSourceName();
  List<PersonRole> query(WorkId id) throws IOException;
}

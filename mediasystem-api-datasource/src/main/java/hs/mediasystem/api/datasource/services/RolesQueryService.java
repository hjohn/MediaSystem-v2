package hs.mediasystem.api.datasource.services;

import hs.mediasystem.api.datasource.domain.PersonRole;
import hs.mediasystem.domain.work.WorkId;

import java.io.IOException;
import java.util.List;

public interface RolesQueryService {
  String getDataSourceName();
  List<PersonRole> query(WorkId id) throws IOException;
}

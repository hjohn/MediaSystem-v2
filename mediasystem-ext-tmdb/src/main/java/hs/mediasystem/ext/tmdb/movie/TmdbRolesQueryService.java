package hs.mediasystem.ext.tmdb.movie;

import hs.mediasystem.api.datasource.domain.PersonRole;
import hs.mediasystem.api.datasource.services.RolesQueryService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class TmdbRolesQueryService implements RolesQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;

  @Override
  public String getDataSourceName() {
    return "TMDB";
  }

  @Override
  public List<PersonRole> query(WorkId id) throws IOException {
    return tmdb.query(idToLocation(id), "text:json:" + id)
      .map(personRoles::toPersonRoles)
      .orElse(List.of());
  }

  private static String idToLocation(WorkId id) {
    if(id.getType() == MediaType.MOVIE) {
      return "3/movie/" + id.getKey() + "/credits";
    }
    if(id.getType() == MediaType.SERIE) {
      return "3/tv/" + id.getKey() + "/credits";
    }
    if(id.getType() == MediaType.EPISODE) {
      String[] parts = id.getKey().split("/");

      return "3/tv/" + parts[0] + "/season/" + parts[1] + "/episode/" + parts[2] + "/credits";
    }

    throw new IllegalArgumentException("Unsupported type: " + id);
  }
}

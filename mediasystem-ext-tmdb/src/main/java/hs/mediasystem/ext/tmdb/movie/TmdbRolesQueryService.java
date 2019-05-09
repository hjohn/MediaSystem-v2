package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.PersonRole;
import hs.mediasystem.ext.basicmediatypes.services.RolesQueryService;
import hs.mediasystem.ext.tmdb.PersonRoles;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.scanner.api.MediaType;

import java.util.List;

import javax.inject.Inject;

public class TmdbRolesQueryService implements RolesQueryService {
  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType SERIE = MediaType.of("SERIE");
  private static final MediaType EPISODE = MediaType.of("EPISODE");

  @Inject private TheMovieDatabase tmdb;
  @Inject private PersonRoles personRoles;

  @Override
  public List<PersonRole> query(Identifier identifier) {
    JsonNode info = tmdb.query(identifierToLocation(identifier));

    return personRoles.toPersonRoles(info);
  }

  private static String identifierToLocation(Identifier identifier) {
    if(identifier.getDataSource().getType() == MOVIE) {
      return "3/movie/" + identifier.getId() + "/credits";
    }
    if(identifier.getDataSource().getType() == SERIE) {
      return "3/tv/" + identifier.getId() + "/credits";
    }
    if(identifier.getDataSource().getType() == EPISODE) {
      String[] parts = identifier.getId().split("/");

      return "3/tv/" + parts[0] + "/season/" + parts[1] + "/episode/" + parts[2] + "/credits";
    }

    throw new IllegalArgumentException("Unsupported identifier: " + identifier);
  }
}

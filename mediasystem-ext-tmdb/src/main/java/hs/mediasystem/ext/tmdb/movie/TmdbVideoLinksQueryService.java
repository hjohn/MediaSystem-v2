package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.Type;
import hs.mediasystem.ext.basicmediatypes.VideoLink;
import hs.mediasystem.ext.basicmediatypes.services.VideoLinksQueryService;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.ext.tmdb.VideoLinks;

import java.util.List;

import javax.inject.Inject;

public class TmdbVideoLinksQueryService implements VideoLinksQueryService {
  private static final Type MOVIE = Type.of("MOVIE");
  private static final Type SERIE = Type.of("SERIE");
  private static final Type EPISODE = Type.of("EPISODE");

  @Inject private TheMovieDatabase tmdb;
  @Inject private VideoLinks videoLinks;

  @Override
  public List<VideoLink> query(Identifier identifier) {
    JsonNode info = tmdb.query(identifierToLocation(identifier));

    return videoLinks.toVideoLinks(info);
  }

  private static String identifierToLocation(Identifier identifier) {
    if(identifier.getDataSource().getType() == MOVIE) {
      return "3/movie/" + identifier.getId() + "/videos";
    }
    if(identifier.getDataSource().getType() == SERIE) {
      return "3/tv/" + identifier.getId() + "/videos";
    }
    if(identifier.getDataSource().getType() == EPISODE) {
      String[] parts = identifier.getId().split("/");

      return "3/tv/" + parts[0] + "/season/" + parts[1] + "/episode/" + parts[2] + "/videos";
    }

    throw new IllegalArgumentException("Unsupported identifier: " + identifier);
  }
}

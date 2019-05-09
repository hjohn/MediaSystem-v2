package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.domain.ProductionIdentifier;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;
import hs.mediasystem.scanner.api.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class TmdbRecommendationQueryService implements RecommendationQueryService {
  private static final MediaType MOVIE = MediaType.of("MOVIE");
  private static final MediaType SERIE = MediaType.of("SERIE");

  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public List<Production> query(ProductionIdentifier identifier) {
    JsonNode info = tmdb.query(identifierToLocation(identifier));
    List<Production> productions = new ArrayList<>();

    for(JsonNode result : info.path("results")) {
      if(identifier.getDataSource().getType() == MOVIE) {
        productions.add(objectFactory.toMovie(result));
      }
      else if(identifier.getDataSource().getType() == SERIE) {
        productions.add(objectFactory.toSerie(result, Collections.emptyList()));
      }
    }

    return productions;
  }

  private static String identifierToLocation(Identifier identifier) {
    if(identifier.getDataSource().getType() == MOVIE) {
      return "3/movie/" + identifier.getId() + "/recommendations";
    }
    if(identifier.getDataSource().getType() == SERIE) {
      return "3/tv/" + identifier.getId() + "/recommendations";
    }

    throw new IllegalArgumentException("Unsupported identifier: " + identifier);
  }
}

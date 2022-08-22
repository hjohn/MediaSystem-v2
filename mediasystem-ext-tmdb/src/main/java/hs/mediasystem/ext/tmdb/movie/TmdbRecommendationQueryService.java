package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.services.RecommendationQueryService;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbRecommendationQueryService implements RecommendationQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public List<Production> query(WorkId id) throws IOException {
    JsonNode info = tmdb.query(idToLocation(id), "text:json:" + id);
    List<Production> productions = new ArrayList<>();

    for(JsonNode result : info.path("results")) {
      if(id.getType() == MediaType.MOVIE) {
        productions.add(objectFactory.toMovie(result));
      }
      else if(id.getType() == MediaType.SERIE) {
        productions.add(objectFactory.toSerie(result, null));
      }
    }

    return productions;
  }

  private static String idToLocation(WorkId id) {
    if(id.getType() == MediaType.MOVIE) {
      return "3/movie/" + id.getKey() + "/recommendations";
    }
    if(id.getType() == MediaType.SERIE) {
      return "3/tv/" + id.getKey() + "/recommendations";
    }

    throw new IllegalArgumentException("Unsupported type: " + id);
  }
}

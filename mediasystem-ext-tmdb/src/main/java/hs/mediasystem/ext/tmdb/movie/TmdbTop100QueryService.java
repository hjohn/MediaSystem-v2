package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.domain.Production;
import hs.mediasystem.ext.basicmediatypes.services.Top100QueryService;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbTop100QueryService implements Top100QueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public List<Production> query() {
    List<Production> productions = new ArrayList<>();

    for(int i = 1; i <= 10; i++) {
      JsonNode info = tmdb.query("3/movie/top_rated", "page", "" + i);  // popular?

      for(JsonNode result : info.path("results")) {
        productions.add(objectFactory.toMovie(result));
      }
    }

    return productions;
  }
}

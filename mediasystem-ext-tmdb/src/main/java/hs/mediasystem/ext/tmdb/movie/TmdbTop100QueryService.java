package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.datasource.services.Top100QueryService;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class TmdbTop100QueryService implements Top100QueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public List<Production> query() throws IOException {
    List<Production> productions = new ArrayList<>();

    for(int i = 1; i <= 10; i++) {
      JsonNode info = tmdb.get("3/movie/top_rated", null, List.of("page", "" + i));  // popular?

      for(JsonNode result : info.path("results")) {
        productions.add(objectFactory.toMovie(result));
      }
    }

    return productions;
  }
}

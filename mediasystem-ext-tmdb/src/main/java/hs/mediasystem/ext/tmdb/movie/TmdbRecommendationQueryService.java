package hs.mediasystem.ext.tmdb.movie;

import hs.mediasystem.api.datasource.domain.Production;
import hs.mediasystem.api.datasource.services.RecommendationQueryService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

public class TmdbRecommendationQueryService implements RecommendationQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public List<Production> query(WorkId id) throws IOException {
    return tmdb.query(idToLocation(id), "text:json:" + id).stream()
      .flatMap(n -> StreamSupport.stream(n.path("results").spliterator(), false))
      .<Production>mapMulti((result, consumer) -> {
        if(id.getType() == MediaType.MOVIE) {
          consumer.accept(objectFactory.toMovie(result));
        }
        else if(id.getType() == MediaType.SERIE) {
          consumer.accept(objectFactory.toSerie(result, null));
        }
      })
      .toList();
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

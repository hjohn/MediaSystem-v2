package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class TmdbQueryService extends AbstractQueryService<Movie> {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  public TmdbQueryService() {
    super(DataSources.TMDB_MOVIE);
  }

  @Override
  public Result<Movie> query(Identifier identifier) {
    JsonNode node = tmdb.query("3/movie/" + identifier.getId()); //, "append_to_response", "credits,videos");  // keywords,alternative_titles,recommendations,similar,reviews
    String imdbId = node.path("imdb_id").textValue();
    Set<Identification> newIdentifications = new HashSet<>();

    if(imdbId != null) {
      newIdentifications.add(new Identification(new Identifier(DataSources.IMDB_MOVIE, imdbId), MatchType.DERIVED, 1.0));
    }

    Movie movie = objectFactory.toMovie(node);

    return Result.of(movie, newIdentifications);
  }
}

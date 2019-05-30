package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class TmdbQueryService extends AbstractQueryService<Movie> {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  public TmdbQueryService() {
    super(DataSources.TMDB_MOVIE);
  }

  @Override
  public Result<Movie> query(Identifier identifier) {
    JsonNode node = tmdb.query("3/movie/" + identifier.getId(), "append_to_response", "keywords");  // keywords,alternative_titles,recommendations,similar,reviews
    String imdbId = node.path("imdb_id").textValue();
    Map<Identifier, Identification> newIdentifications = new HashMap<>();

    if(imdbId != null) {
      Identifier newIdentifier = new Identifier(DataSources.IMDB_MOVIE, imdbId);

      newIdentifications.put(newIdentifier, new Identification(MatchType.DERIVED, 1.0, Instant.now()));
    }

    JsonNode collectionPath = node.path("belongs_to_collection");

    if(collectionPath.isObject()) {
      newIdentifications.put(
        new Identifier(DataSources.TMDB_COLLECTION, collectionPath.path("id").asText()),
        new Identification(MatchType.DERIVED, 1.0, Instant.now())
      );
    }

    Movie movie = objectFactory.toMovie(node);

    return Result.of(movie, newIdentifications);
  }
}

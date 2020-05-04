package hs.mediasystem.ext.tmdb.movie;

import com.fasterxml.jackson.databind.JsonNode;

import hs.mediasystem.ext.basicmediatypes.domain.Identifier;
import hs.mediasystem.ext.basicmediatypes.domain.Movie;
import hs.mediasystem.ext.basicmediatypes.services.AbstractQueryService;
import hs.mediasystem.ext.tmdb.DataSources;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class TmdbQueryService extends AbstractQueryService {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  public TmdbQueryService() {
    super(DataSources.TMDB_MOVIE);
  }

  @Override
  public Movie query(Identifier identifier) throws IOException {
    JsonNode node = tmdb.query("3/movie/" + identifier.getId(), "text:json:" + identifier, List.of("append_to_response", "keywords,release_dates"));  // keywords,alternative_titles,recommendations,similar,reviews

    return objectFactory.toMovie(node);
  }
}

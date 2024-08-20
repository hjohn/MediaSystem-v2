package hs.mediasystem.ext.tmdb.provider;

import hs.mediasystem.api.datasource.domain.Movie;
import hs.mediasystem.ext.tmdb.ObjectFactory;
import hs.mediasystem.ext.tmdb.TheMovieDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MovieProvider implements MediaProvider<Movie> {
  @Inject private TheMovieDatabase tmdb;
  @Inject private ObjectFactory objectFactory;

  @Override
  public Optional<Movie> provide(String key) throws IOException {
    // keywords,alternative_titles,recommendations,similar,reviews
    return tmdb.query("3/movie/" + key, "text:json:tmdb:movie:" + key, List.of("append_to_response", "keywords,release_dates"))
      .map(objectFactory::toMovie);
  }
}

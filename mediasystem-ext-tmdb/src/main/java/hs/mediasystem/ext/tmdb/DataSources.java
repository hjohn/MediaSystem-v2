package hs.mediasystem.ext.tmdb;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.Type;

public class DataSources {
  public static final DataSource TMDB_MOVIE = DataSource.instance(Type.of("MOVIE"), "TMDB");
  public static final DataSource TMDB_SERIE = DataSource.instance(Type.of("SERIE"), "TMDB");
  public static final DataSource TMDB_SEASON = DataSource.instance(Type.of("SEASON"), "TMDB");
  public static final DataSource TMDB_EPISODE = DataSource.instance(Type.of("EPISODE"), "TMDB");

  public static final DataSource IMDB_MOVIE = DataSource.instance(Type.of("MOVIE"), "IMDB");
  public static final DataSource IMDB_SERIE = DataSource.instance(Type.of("SERIE"), "IMDB");
  public static final DataSource IMDB_EPISODE = DataSource.instance(Type.of("EPISODE"), "IMDB");

  public static final DataSource TMDB_PERSON = DataSource.instance(Type.of("PERSON"), "TMDB");
  public static final DataSource TMDB_CREDIT = DataSource.instance(Type.of("CREDIT"), "TMDB");
}

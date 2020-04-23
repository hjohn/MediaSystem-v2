package hs.mediasystem.ext.tmdb;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;

public class DataSources {
  public static final DataSource TMDB_MOVIE = DataSource.instance(MediaType.MOVIE, "TMDB");
  public static final DataSource TMDB_SERIE = DataSource.instance(MediaType.SERIE, "TMDB");
  public static final DataSource TMDB_SEASON = DataSource.instance(MediaType.SEASON, "TMDB");
  public static final DataSource TMDB_EPISODE = DataSource.instance(MediaType.EPISODE, "TMDB");

  public static final DataSource IMDB_MOVIE = DataSource.instance(MediaType.MOVIE, "IMDB");
  public static final DataSource IMDB_SERIE = DataSource.instance(MediaType.SERIE, "IMDB");
  public static final DataSource IMDB_EPISODE = DataSource.instance(MediaType.EPISODE, "IMDB");

  public static final DataSource TMDB_PERSON = DataSource.instance(MediaType.PERSON, "TMDB");
  public static final DataSource TMDB_CREDIT = DataSource.instance(MediaType.CREDIT, "TMDB");
  public static final DataSource TMDB_COLLECTION = DataSource.instance(MediaType.COLLECTION, "TMDB");
  public static final DataSource TMDB_KEYWORD = DataSource.instance(MediaType.KEYWORD, "TMDB");
}

package hs.mediasystem.ext.tmdb;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;

public class DataSources {
  public static final DataSource TMDB_MOVIE = DataSource.instance(MediaType.of("MOVIE"), "TMDB");
  public static final DataSource TMDB_SERIE = DataSource.instance(MediaType.of("SERIE"), "TMDB");
  public static final DataSource TMDB_SEASON = DataSource.instance(MediaType.of("SEASON"), "TMDB");
  public static final DataSource TMDB_EPISODE = DataSource.instance(MediaType.of("EPISODE"), "TMDB");

  public static final DataSource IMDB_MOVIE = DataSource.instance(MediaType.of("MOVIE"), "IMDB");
  public static final DataSource IMDB_SERIE = DataSource.instance(MediaType.of("SERIE"), "IMDB");
  public static final DataSource IMDB_EPISODE = DataSource.instance(MediaType.of("EPISODE"), "IMDB");

  public static final DataSource TMDB_PERSON = DataSource.instance(MediaType.of("PERSON"), "TMDB");
  public static final DataSource TMDB_CREDIT = DataSource.instance(MediaType.of("CREDIT"), "TMDB");
  public static final DataSource TMDB_COLLECTION = DataSource.instance(MediaType.of("COLLECTION"), "TMDB");
  public static final DataSource TMDB_KEYWORD = DataSource.instance(MediaType.of("KEYWORD"), "TMDB");
}

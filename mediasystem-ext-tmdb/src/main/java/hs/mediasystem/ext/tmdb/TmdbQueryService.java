package hs.mediasystem.ext.tmdb;

import hs.mediasystem.api.datasource.WorkDescriptor;
import hs.mediasystem.api.datasource.services.QueryService;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.ext.tmdb.provider.CollectionProvider;
import hs.mediasystem.ext.tmdb.provider.EpisodeProvider;
import hs.mediasystem.ext.tmdb.provider.MovieProvider;
import hs.mediasystem.ext.tmdb.provider.SerieProvider;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TmdbQueryService implements QueryService {
  @Inject private MovieProvider movieProvider;
  @Inject private SerieProvider serieProvider;
  @Inject private CollectionProvider collectionProvider;
  @Inject private EpisodeProvider episodeProvider;

  @Override
  public DataSource getDataSource() {
    return DataSources.TMDB;
  }

  @Override
  public Optional<? extends WorkDescriptor> query(WorkId id) throws IOException {
    return switch(id.getType()) {
      case MediaType.MOVIE -> movieProvider.provide(id.getKey());
      case MediaType.SERIE -> serieProvider.provide(id.getKey());
      case MediaType.COLLECTION -> collectionProvider.provide(id.getKey());
      case MediaType.EPISODE -> episodeProvider.provide(id.getKey());
      default -> throw new UnsupportedOperationException("MediaType unsupported by TMDB provider: " + id.getType());
    };
  }
}

package hs.mediasystem.api.datasource.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;

public abstract class AbstractQueryService implements QueryService {
  private final DataSource dataSource;
  private final MediaType mediaType;

  public AbstractQueryService(DataSource dataSource, MediaType mediaType) {
    this.dataSource = dataSource;
    this.mediaType = mediaType;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public MediaType getMediaType() {
    return mediaType;
  }
}

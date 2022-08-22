package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;

public abstract class AbstractIdentificationService implements IdentificationService {
  private final DataSource dataSource;
  private final MediaType mediaType;

  public AbstractIdentificationService(DataSource dataSource, MediaType mediaType) {
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

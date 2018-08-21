package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.DataSource;
import hs.mediasystem.ext.basicmediatypes.scan.MediaDescriptor;

public abstract class AbstractQueryService<T extends MediaDescriptor> implements QueryService<T> {
  private final DataSource dataSource;

  public AbstractQueryService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}

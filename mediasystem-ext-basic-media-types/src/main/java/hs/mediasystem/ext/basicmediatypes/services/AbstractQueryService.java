package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.domain.work.DataSource;

public abstract class AbstractQueryService implements QueryService {
  private final DataSource dataSource;

  public AbstractQueryService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}

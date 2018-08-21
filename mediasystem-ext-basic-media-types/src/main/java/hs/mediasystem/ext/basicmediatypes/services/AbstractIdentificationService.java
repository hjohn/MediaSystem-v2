package hs.mediasystem.ext.basicmediatypes.services;

import hs.mediasystem.ext.basicmediatypes.DataSource;

public abstract class AbstractIdentificationService implements IdentificationService {
  private final DataSource dataSource;

  public AbstractIdentificationService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}

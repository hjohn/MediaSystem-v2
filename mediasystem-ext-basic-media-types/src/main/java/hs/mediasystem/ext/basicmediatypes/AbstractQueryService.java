package hs.mediasystem.ext.basicmediatypes;

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

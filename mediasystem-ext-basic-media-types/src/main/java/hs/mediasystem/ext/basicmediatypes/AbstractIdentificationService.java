package hs.mediasystem.ext.basicmediatypes;

public abstract class AbstractIdentificationService<T extends MediaStream<?>> implements IdentificationService<T> {
  private final DataSource dataSource;

  public AbstractIdentificationService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}

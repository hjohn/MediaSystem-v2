package hs.mediasystem.mediamanager;

import hs.mediasystem.domain.work.DataSource;

import java.util.Objects;

public class UnknownDataSourceException extends RuntimeException {
  private final DataSource dataSource;

  public UnknownDataSourceException(DataSource dataSource) {
    super("" + dataSource);

    if(dataSource == null) {
      throw new IllegalArgumentException("dataSource cannot be null");
    }

    this.dataSource = dataSource;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataSource);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    UnknownDataSourceException other = (UnknownDataSourceException)obj;

    if(!dataSource.equals(other.dataSource)) {
      return false;
    }

    return true;
  }
}

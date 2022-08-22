package hs.mediasystem.domain.work;

import java.util.Objects;

public abstract class AbstractId {
  private final DataSource dataSource;
  private final String key;

  public AbstractId(DataSource dataSource, String key) {
    if(dataSource == null) {
      throw new IllegalArgumentException("dataSource cannot be null");
    }
    if(key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }

    this.dataSource = dataSource;
    this.key = key;
  }

  public final DataSource getDataSource() {
    return dataSource;
  }

  public final String getKey() {
    return key;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(dataSource, key);
  }

  @Override
  public final boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AbstractId other = (AbstractId)obj;

    if(!key.equals(other.key)) {
      return false;
    }
    if(!dataSource.equals(other.dataSource)) {
      return false;
    }

    return true;
  }

  @Override
  public final String toString() {
    return dataSource.toString() + ":" + key;
  }
}

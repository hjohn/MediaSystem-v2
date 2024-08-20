package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.MediaType;

import java.util.Objects;

public class WorkId {
  private final DataSource dataSource;
  private final MediaType type;
  private final String key;

  public WorkId(DataSource dataSource, MediaType type, String key) {
    if(dataSource == null) {
      throw new IllegalArgumentException("dataSource cannot be null");
    }
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }

    this.dataSource = dataSource;
    this.type = type;
    this.key = key;
  }

  public final DataSource getDataSource() {
    return dataSource;
  }

  public final MediaType getType() {
    return type;
  }

  public final String getKey() {
    return key;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(dataSource, key, type);
  }

  @Override
  public final boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    WorkId other = (WorkId)obj;

    if(!key.equals(other.key)) {
      return false;
    }
    if(!dataSource.equals(other.dataSource)) {
      return false;
    }
    if(!type.equals(other.type)) {
      return false;
    }

    return true;
  }

  @Override
  public final String toString() {
    return dataSource + ":" + type + ":" + key;
  }
}

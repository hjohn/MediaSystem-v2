package hs.mediasystem.ext.basicmediatypes.domain;

import hs.mediasystem.domain.work.DataSource;

import java.util.Objects;

public class Identifier {  // TODO refactor
  private final DataSource dataSource;
  private final String id;

  public Identifier(DataSource dataSource, String id) {
    if(dataSource == null) {
      throw new IllegalArgumentException("dataSource cannot be null");
    }
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }

    this.dataSource = dataSource;
    this.id = id;
  }

  public Identifier getRootIdentifier() {
    return null;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public String getId() {
    return id;
  }

  public static Identifier fromString(String key) {
    int colon = key.lastIndexOf(":");

    return new Identifier(DataSource.fromString(key.substring(0, colon)), key.substring(colon + 1));
  }

  @Override
  public String toString() {
    return dataSource.toString() + ":" + id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataSource, id);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {  /* temporary commented out until Identifiers have been converted to ProductionIdentifiers: TODO: || getClass() != obj.getClass()) { */
      return false;
    }

    Identifier other = (Identifier)obj;

    if(!dataSource.equals(other.dataSource)) {
      return false;
    }
    if(!id.equals(other.id)) {
      return false;
    }

    return true;
  }
}

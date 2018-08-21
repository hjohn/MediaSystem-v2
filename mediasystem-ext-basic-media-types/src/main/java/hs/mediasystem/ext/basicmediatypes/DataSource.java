package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.ext.basicmediatypes.domain.Type;

import java.util.HashMap;
import java.util.Map;

public class DataSource {
  private static final Map<String, DataSource> knownDataSources = new HashMap<>();

  private final Type type;
  private final String name;

  public static DataSource instance(Type type, String name) {
    return knownDataSources.computeIfAbsent(name + ":" + type, k -> new DataSource(type, name));
  }

  public static DataSource fromString(String key) {
    String[] parts = key.split(":");

    return DataSource.instance(Type.of(parts[1]), parts[0]);
  }

  private DataSource(Type type, String name) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    this.type = type;
    this.name = name;
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return name + ":" + type;
  }

//  @Override
//  public int hashCode() {
//    return Objects.hash(type, name);
//  }
//
//  @Override
//  public boolean equals(Object obj) {
//    if(this == obj) {
//      return true;
//    }
//    if(obj == null || getClass() != obj.getClass()) {
//      return false;
//    }
//
//    DataSource other = (DataSource)obj;
//
//    if(!type.equals(other.type)) {
//      return false;
//    }
//    if(!name.equals(other.name)) {
//      return false;
//    }
//
//    return true;
//  }
}

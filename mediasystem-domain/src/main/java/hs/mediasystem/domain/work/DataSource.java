package hs.mediasystem.domain.work;

import hs.mediasystem.domain.stream.MediaType;

import java.util.HashMap;
import java.util.Map;

public class DataSource {
  private static final Map<String, DataSource> knownDataSources = new HashMap<>();

  private final MediaType type;
  private final String name;

  public static DataSource instance(MediaType type, String name) {
    return knownDataSources.computeIfAbsent(name + ":" + type, k -> new DataSource(type, name));
  }

  public static DataSource fromString(String key) {
    String[] parts = key.split(":");

    return DataSource.instance(MediaType.of(parts[1]), parts[0]);
  }

  private DataSource(MediaType type, String name) {
    if(type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if(name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    this.type = type;
    this.name = name;
  }

  public MediaType getType() {
    return type;
  }

  public String getName() {
    return name;
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

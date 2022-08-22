package hs.mediasystem.domain.work;

import java.util.HashMap;
import java.util.Map;

public class DataSource {
  private static final Map<String, DataSource> knownDataSources = new HashMap<>();

  private final String name;

  public static DataSource instance(String name) {
    return knownDataSources.computeIfAbsent(name, k -> new DataSource(name));
  }

  private DataSource(String name) {
    if(name == null || name.isBlank()) {
      throw new IllegalArgumentException("name cannot be null or blank: " + name);
    }

    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}

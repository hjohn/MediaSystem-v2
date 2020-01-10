package hs.mediasystem.domain.stream;

import java.util.HashMap;
import java.util.Map;

public class MediaType {
  private static final Map<String, MediaType> KNOWN_TYPES = new HashMap<>();

  private final String name;

  public static MediaType of(String name) {
    return KNOWN_TYPES.computeIfAbsent(name, k -> new MediaType(name));
  }

  private MediaType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}

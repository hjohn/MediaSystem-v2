package hs.mediasystem.ext.basicmediatypes;

import java.util.HashMap;
import java.util.Map;

public class Type {
  private static final Map<String, Type> KNOWN_TYPES = new HashMap<>();

  private final String name;

  public static Type of(String name) {
    return KNOWN_TYPES.computeIfAbsent(name, k -> new Type(name));
  }

  private Type(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}

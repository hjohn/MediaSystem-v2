package hs.mediasystem.util.expose;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ExposedControl {
  static final Map<Class<?>, List<ExposedControl>> EXPOSED_PROPERTIES = new HashMap<>();

  public enum Type {METHOD, BOOLEAN, NUMERIC, LIST}

  public static List<ExposedControl> find(Class<?> cls) {
    return EXPOSED_PROPERTIES.getOrDefault(cls, Collections.emptyList());
  }

  public static void clear() {
    EXPOSED_PROPERTIES.clear();
  }

  String getName();
  Class<?> getDeclaringClass();
}


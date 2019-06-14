package hs.mediasystem.framework.expose;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ExposedControl<P> {
  final static Map<Class<?>, List<ExposedControl<?>>> EXPOSED_PROPERTIES = new HashMap<>();

  public enum Type {METHOD, BOOLEAN, NUMERIC, LIST}

  @SuppressWarnings("unchecked")
  public static <P> List<ExposedControl<P>> find(Class<P> cls) {
    return (List<ExposedControl<P>>)(List<?>)EXPOSED_PROPERTIES.getOrDefault(cls, Collections.emptyList());
  }

  @SuppressWarnings("unchecked")
  public static <P> ExposedControl<P> find(Class<P> cls, String name) {
    return (ExposedControl<P>)EXPOSED_PROPERTIES.getOrDefault(cls, Collections.emptyList()).stream()
      .filter(ec -> ec.getName().equals(name))
      .findFirst()
      .orElse(null);
  }

  public static void clear() {
    EXPOSED_PROPERTIES.clear();
  }

  String getName();
  Class<P> getDeclaringClass();
}


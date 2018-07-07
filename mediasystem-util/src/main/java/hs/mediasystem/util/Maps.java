package hs.mediasystem.util;

import java.util.HashMap;
import java.util.Map;

public class Maps {

  public static <K, V> Map<K, V> create(K key, V value) {
    return new HashMap<K, V>() {{
      put(key, value);
    }};
  }

  public static <K, V> Map<K, V> create(K k1, V v1, K k2, V v2) {
    return new HashMap<K, V>() {{
      put(k1, v1);
      put(k2, v2);
    }};
  }
}

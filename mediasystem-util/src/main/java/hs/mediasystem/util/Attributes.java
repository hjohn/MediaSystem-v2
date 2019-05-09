package hs.mediasystem.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Attributes {
  private final Map<String, Object> data;

  public static Attributes of(String key, Object value, Object... moreKeyValuePairs) {
    if(moreKeyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("moreKeyValuePairs must be an even size");
    }

    Map<String, Object> map = new HashMap<>();

    map.put(key, value);

    for(int i = 0; i < moreKeyValuePairs.length; i += 2) {
      Object k = moreKeyValuePairs[i];
      Object v = moreKeyValuePairs[i + 1];

      if(!(k instanceof String)) {
        throw new IllegalArgumentException("moreKeyValuePairs element " + i + " must be a String: " + k);
      }
      if(v != null) {
        map.put((String)k, v);
      }
    }

    return new Attributes(map);
  }

  public Attributes(Map<String, Object> data) {
    if(data == null) {
      throw new IllegalArgumentException("data cannot be null");
    }

    this.data = Collections.unmodifiableMap(new HashMap<>(data));
  }

  public Set<String> keySet() {
    return Collections.unmodifiableSet(data.keySet());
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T)data.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key, T defaultValue) {
    return (T)data.getOrDefault(key, defaultValue);
  }

  public Integer asInteger(String year) {
    String v = get(year);

    return v == null ? null : Integer.parseInt(v);
  }

  public boolean contains(String key) {
    return data.containsKey(key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Attributes other = (Attributes)obj;

    if(!data.equals(other.data)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return data.toString();
  }
}

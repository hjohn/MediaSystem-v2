package hs.mediasystem.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Attributes2 {
  private final Map<Attribute, Object> data;

  public Attributes2(Attribute attribute, Object value, Object... moreAttributeValuePairs) {
    if(moreAttributeValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException("moreAttributeValuePairs must be an even size");
    }

    Map<Attribute, Object> map = new HashMap<>();

    map.put(attribute, value);

    for(int i = 0; i < moreAttributeValuePairs.length; i += 2) {
      Object key = moreAttributeValuePairs[i];
      Object v = moreAttributeValuePairs[i + 1];

      if(!(key instanceof Attribute)) {
        throw new IllegalArgumentException("moreAttributeValuePairs element " + i + " must be an Attribute: " + key);
      }
      if(v != null) {
        map.put((Attribute)key, v);
      }
    }

    this.data = Collections.unmodifiableMap(map);
  }

  public Attributes2(Map<Attribute, Object> data) {
    this.data = Collections.unmodifiableMap(new HashMap<>(data));
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Attribute key) {
    return (T)data.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Attribute key, T defaultValue) {
    return (T)data.getOrDefault(key, defaultValue);
  }

  public boolean contains(Attribute key) {
    return data.containsKey(key);
  }
}

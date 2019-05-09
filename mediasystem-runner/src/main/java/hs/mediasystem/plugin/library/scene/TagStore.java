package hs.mediasystem.plugin.library.scene;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class TagStore {
  private static final Map<String, Map<String, String>> data = new HashMap<>();

  public <T> T get(String id, Tag<T> tag) {
    return tag.fromString(data.computeIfAbsent(id, k -> new HashMap<>()).get(tag.getName()));
  }

  public <T> void put(String id, Tag<T> tag, T value) {
    data.computeIfAbsent(id, k -> new HashMap<>()).put(tag.getName(), tag.toString(value));
  }
}

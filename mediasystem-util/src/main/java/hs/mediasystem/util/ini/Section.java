package hs.mediasystem.util.ini;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Section implements Iterable<String> {
  private final String name;
  private final Section parent;
  private final Map<String, List<String>> values = new LinkedHashMap<>();

  public Section(String name, Section parent) {
    this.name = name;
    this.parent = parent;
  }

  public String getName() {
    return name;
  }

  public void put(String key, String value) {
    List<String> list = values.get(key);

    if(list == null) {
      list = new ArrayList<>();
      values.put(key, list);
    }
    list.add(value);
  }

  public String getDefault(String key, String defaultValue) {
    List<String> list = getAll(key);

    return list.isEmpty() ? defaultValue : list.get(0);
  }

  public String get(String key) {
    return getDefault(key, null);
  }

  public List<String> getAll(String key) {
    List<String> results = new ArrayList<>();

    if(values.containsKey(key)) {
      results.addAll(values.get(key));
    }

    if(parent != null) {
      results.addAll(parent.getAll(key));
    }

    return results;
  }

  @Override
  public Iterator<String> iterator() {
    return values.keySet().iterator();
  }
}

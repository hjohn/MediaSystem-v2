package hs.mediasystem.mediamanager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BiMultiMap<A, B, T> {
  private final Map<A, Map<B, T>> map1 = new HashMap<>();
  private final Map<B, Map<A, T>> map2 = new HashMap<>();

  public void associate(A a, B b, T data) {
    map1.computeIfAbsent(a, k -> new HashMap<>()).put(b, data);
    map2.computeIfAbsent(b, k -> new HashMap<>()).put(a, data);
  }

  public T unassociate(A a, B b) {
    Map<B, T> map = map1.get(a);

    if(map == null) {
      return null;
    }

    T data = map.get(b);

    map1.computeIfPresent(a, (k, v) -> v.remove(b) != null && v.isEmpty() ? null : v);
    map2.computeIfPresent(b, (k, v) -> v.remove(a) != null && v.isEmpty() ? null : v);

    return data;
  }

  public Set<A> getLeftSet(B b) {
    Map<A, T> map = map2.get(b);

    if(map == null) {
      return new HashSet<>();
    }

    return new HashSet<>(map.keySet());
  }

  public Set<B> getRightSet(A a) {
    Map<B, T> map = map1.get(a);

    if(map == null) {
      return new HashSet<>();
    }

    return new HashSet<>(map.keySet());
  }

  public Map<A, T> getLeftMap(B b) {
    Map<A, T> map = map2.get(b);

    if(map == null) {
      return new HashMap<>();
    }

    return new HashMap<>(map);
  }

  public Map<B, T> getRightMap(A a) {
    Map<B, T> map = map1.get(a);

    if(map == null) {
      return new HashMap<>();
    }

    return new HashMap<>(map);
  }

  public boolean contains(A a, B b) {
    Map<B, T> map = map1.get(a);

    if(map == null) {
      return false;
    }

    return map.containsKey(b);
  }

  @Override
  public String toString() {
    return "BiMultiMap[\n - " + map1.toString() + "\n - " + map2.toString() + "\n]";
  }
}

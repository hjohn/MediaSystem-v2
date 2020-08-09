package hs.database.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WeakValueMap<K, V> implements Map<K, V> {
  private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();
  private final Map<K, KeyedWeakReference<K, V>> map = new HashMap<>();

  /*
   * Important implementation notes:
   *
   * When a KeyedWeakReference is removed, the key is still intact -- if this later gets enqueue'd, this
   * may trigger another removal of said key, which, in the mean time, can be used for a different,
   * unrelated, value.  Therefore it is essential that when removed the KeyedWeakReference is rendered
   * inactive.
   *
   * Also note that methods like isEmpty() and size() will not accurately reflect the number of active
   * values in this map as some of them may have been garbage collected already.
   */

  @Override
  public V put(K key, V value) {
    if(key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }

    cleanReferenceQueue();

    KeyedWeakReference<K, V> oldValueRef = map.put(key, new KeyedWeakReference<>(key, value, referenceQueue));

    if(oldValueRef != null) {
      V v = oldValueRef.get();
      oldValueRef.clear();  // important to clear it as otherwise it may remove the key later when it gets enqueue'd

      return v;
    }

    return null;
  }

  @Override
  public V get(Object key) {
    WeakReference<V> valueRef = map.get(key);

    return valueRef == null ? null : valueRef.get();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean containsValue(Object value) {
    return map.containsValue(new KeyedWeakReference<>((K)null, (V)value, referenceQueue));
  }

  private void cleanReferenceQueue() {
    for(;;) {
      @SuppressWarnings("unchecked")
      KeyedWeakReference<K, V> ref = (KeyedWeakReference<K, V>)referenceQueue.poll();

      if(ref == null) {
        break;
      }

      if(ref.getKey() != null) {
        map.remove(ref.getKey());
      }
    }
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public V remove(Object key) {
    cleanReferenceQueue();

    WeakReference<V> valueRef = map.remove(key);

    if(valueRef != null) {
      V v = valueRef.get();
      valueRef.clear();  // important to clear it as otherwise it may remove the key later when it gets enqueue'd

      return v;
    }

    return null;
  }

  @Override
  public void clear() {
    for(Map.Entry<K, KeyedWeakReference<K, V>> entry : map.entrySet()) {
      entry.getValue().clear();
    }
    map.clear();
    cleanReferenceQueue();
  }

  @Override
  public Set<K> keySet() {
    return map.keySet();
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }
}

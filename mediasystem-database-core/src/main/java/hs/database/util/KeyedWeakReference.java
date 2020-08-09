package hs.database.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class KeyedWeakReference<K, V> extends WeakReference<V> {
  private final K key;
  private final int hashCode;

  public KeyedWeakReference(K key, V referent, ReferenceQueue<? super V> q) {
    super(referent, q);
    this.key = key;
    this.hashCode = referent.hashCode();
  }

  public K getKey() {
    return key;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    @SuppressWarnings("unchecked")
    KeyedWeakReference<K, V> other = (KeyedWeakReference<K, V>)obj;

    V v1 = get();
    V v2 = other.get();

    if(v1 == null || v2 == null || v1.getClass() != v2.getClass()) {
      return false;
    }

    return v1 == v2 || v1.equals(v2);
  }
}
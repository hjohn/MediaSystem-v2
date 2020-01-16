package hs.mediasystem.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Keeps references to objects until they need to be GC'd.  Soft references are kept as long as objects meet
 * the cache limits, while weak references are kept for objects that would exceed these limits.
 */
public class Cache {
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
  private final TreeMap<Instant, Map<Object, Item>> lru = new TreeMap<>();  // in LRU order
  private final Map<Object, Item> byKey = new HashMap<>();

  private final long maxSize;
  private final long maxTTLMillis;

  private long approximateTotalSize;

  public Cache(long maxSize, long maxTTLMillis) {
    this.maxSize = maxSize;
    this.maxTTLMillis = maxTTLMillis;
  }

  public synchronized void add(Object key, Object obj, long approximateSize) {
    cleanReferenceQueue();

    Item item = byKey.get(key);

    if(item != null) {
      // Remove item at old time position from LRU:
      lru.computeIfPresent(item.time, (k, v) -> v.remove(key) != null && v.isEmpty() ? null : v);
      approximateTotalSize -= item.approximateSize;
    }

    Instant now = Instant.now();

    item = new Item(now, key, approximateSize, obj);  // If item was Weak before, it will be Soft again now

    byKey.put(key, item);
    lru.computeIfAbsent(now, k -> new HashMap<>()).put(key, item);
    approximateTotalSize += approximateSize;

    ensureCacheInvariants();
  }

  private void ensureCacheInvariants() {

    /*
     * Walk through oldest entries until both max TTL and approximate total size are within the
     * cache limits.
     */

    Instant oldestAllowed = Instant.now().minusMillis(maxTTLMillis);

    for(Map.Entry<Instant, Map<Object, Item>> entry : lru.entrySet()) {
      for(Item item : entry.getValue().values()) {
        if(item.time.isAfter(oldestAllowed) && approximateTotalSize < maxSize) {
          break;
        }

        if(item.isSoftReference()) {
          item.toWeakReference();
        }
      }
    }
  }

  private void cleanReferenceQueue() {
    for(;;) {
      KeyedReference ref = (KeyedReference)referenceQueue.poll();

      if(ref == null) {
        break;
      }

      Item item = byKey.remove(ref.getKey());

      if(item != null) {
        lru.computeIfPresent(item.time, (k, v) -> v.remove(ref.getKey()) != null && v.isEmpty() ? null : v);
        approximateTotalSize -= item.approximateSize;
      }
    }
  }

  private class Item {
    final Instant time;
    final Object key;

    long approximateSize;
    Reference<Object> reference;

    public Item(Instant time, Object key, long approximateSize, Object obj) {
      this.time = time;
      this.key = key;
      this.approximateSize = approximateSize;
      this.reference = new KeyedSoftReference<>(key, obj);
    }

    public void toWeakReference() {
      Object referent = reference.get();

      if(referent != null) {
        reference = new KeyedWeakReference<>(key, referent);
      }

      /*
       * Weak references no longer count for the size of the cache,
       * so remove its size immediately and set it to zero.  This
       * also ensures the size of this object isn't substracted from
       * the total again when it is processed as part of the reference queue.
       */

      approximateTotalSize -= approximateSize;
      approximateSize = 0;
    }

    public boolean isSoftReference() {
      return reference instanceof KeyedSoftReference;
    }
  }

  private interface KeyedReference {
    Object getKey();
  }

  private class KeyedSoftReference<T> extends SoftReference<T> implements KeyedReference {
    private Object key;

    public KeyedSoftReference(Object key, T referent) {
      super(referent, referenceQueue);

      this.key = key;
    }

    @Override
    public Object getKey() {
      return key;
    }
  }

  private class KeyedWeakReference<T> extends WeakReference<T> implements KeyedReference {
    private Object key;

    public KeyedWeakReference(Object key, T referent) {
      super(referent, referenceQueue);

      this.key = key;
    }

    @Override
    public Object getKey() {
      return key;
    }
  }
}

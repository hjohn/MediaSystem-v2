package hs.mediasystem.util.events.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache for caching an indexed list of items that may contain gaps.
 *
 * @param <T> the item type
 */
class RangeCache<T> {  // ranges are not really exposed, different name when making public?

  /**
   * A cache of ranges. The following always hold true for this cache:
   * - The last entry in the cache always has a value of null
   * - The key of each entry is the same as the range start of its value (not necessarily that of its first item!)
   * - A key can never be part of two ranges
   */
  private final NavigableMap<Long, Range> cache = new TreeMap<>();

  private final Function<T, Long> indexExtractor;

  public RangeCache(Function<T, Long> indexExtractor) {
    this.indexExtractor = indexExtractor;

    cache.put(Long.MIN_VALUE, null);
  }

  /**
   * Returns the first item with an index equal to or greater than the given {@code fromIndex}
   * if it is part of the cache. If not, returns {@code null}.
   *
   * @param fromIndex an index to search from
   * @return the first item with an index equal to or greater than the given
   *   {@code fromIndex}, or {@code null} if no such item is part of the cache
   */
  public synchronized T find(long fromIndex) {
    Range range = cache.floorEntry(fromIndex).getValue();

    if(range == null) {
      return null;
    }

    return range.get(fromIndex);
  }

  /**
   * Inserts a list of items into the cache. The start offset must be
   * equal to or less than the index of the first item, the items must be
   * in ascending order and may not contain duplicate indices.
   *
   * @param start a start index
   * @param items a list of items, cannot be {@code null}
   * @throws IllegalArgumentException when the list of items is not in ascending order,
   *   contains duplicate indices or indices before the given start
   */
  public synchronized void insert(long start, List<T> items) {
    Range newRange = new Range(start, items);

    Entry<Long, Range> nextEntry = cache.floorEntry(newRange.end + 1);

    Range previous = cache.floorEntry(start - 1).getValue();
    Range next = nextEntry.getValue();

    if(previous != null) {
      // merge previous range with new range:
      newRange = previous.merge(newRange);
    }

    if(next != null) {
      // merge next range with new range:
      newRange = next.merge(newRange);
    }
    else {
      // add new empty range after insertion:
      cache.put(newRange.end + 1, null);
    }

    cache.subMap(newRange.start, newRange.end + 1).clear();
    cache.put(newRange.start, newRange);
  }

  class Range {
    long start;  // can be before the first item index
    long end;    // always exactly last item index (inclusive end)
    List<T> items;

    Range(long start, List<T> items) {
      if(items == null || items.isEmpty()) {
        throw new IllegalArgumentException("items cannot be null or empty: " + items);
      }

      long previousIndex = Long.MIN_VALUE;

      for(int i = 0; i < items.size(); i++) {
        T item = items.get(i);
        long index = indexExtractor.apply(item);

        if(index < start) {
          throw new IllegalArgumentException("items cannot contain items before start (" + start + "): " + item);
        }
        if(index <= previousIndex) {
          throw new IllegalArgumentException("items must be in ascending order and contain no duplicate indices: " + items);
        }

        previousIndex = index;
      }

      this.start = start;
      this.end = indexExtractor.apply(items.get(items.size() - 1));
      this.items = items;
    }

    T get(long fromIndex) {
      if(fromIndex < start || fromIndex > end) {
        throw new IllegalArgumentException("fromIndex must be between " + start + " and " + end + ": " + fromIndex);
      }

      return binarySearch(fromIndex);
    }

    private T binarySearch(long fromIndex) {
      int low = 0;
      int high = items.size() - 1;

      while(low <= high) {
        int mid = (low + high) >>> 1;
        T item = items.get(mid);
        int cmp = Long.compare(indexExtractor.apply(item), fromIndex);

        if(cmp == 0) {
          return item;
        }

        if(cmp < 0) {
          low = mid + 1;
        }
        else {
          high = mid - 1;
        }
      }

      return items.get(low);
    }

    Range merge(Range other) {
      if(other.start > end + 1 || other.end < start - 1) {
        throw new IllegalArgumentException("cannot merge non-overlapping or non-touching ranges: " + this + " + " + other);
      }

      Range first = other.start < start ? other : this;
      Range second = other.start < start ? this : other;

      List<T> merged = new ArrayList<>(first.items);
      long end = first.end;

      for(int i = 0; i < second.items.size(); i++) {
        T item = second.items.get(i);

        if(indexExtractor.apply(item) > end) {
          merged.add(item);
        }
      }

      return new Range(first.start, merged);
    }

    @Override
    public String toString() {
      return items.stream().map(indexExtractor::apply).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
    }
  }
}

package hs.mediasystem.db.core;

import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.util.events.EventSelector;
import hs.mediasystem.util.events.PersistentEventStream;
import hs.mediasystem.util.events.store.EventStore;
import hs.mediasystem.util.events.streams.Source;
import hs.mediasystem.util.events.streams.Subscription;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Produces;

@Singleton
public class StreamableService {

  /**
   * Comparator which mimics the behavior of {@code Comparator.compare(s -> s + "/")}
   * without the overhead of creating new strings. A quick performance measurement
   * showed this is about twice as fast.
   */
  private static final Comparator<String> PATH_COMPARATOR = new Comparator<>() {
    @Override
    public int compare(String o1, String o2) {
      int l1 = o1.length();
      int l2 = o2.length();
      int min = Math.min(l1, l2);

      for(int i = 0; i < min; i++) {
        int c = Character.compare(o1.charAt(i), o2.charAt(i));

        if(c != 0) {
          return c;
        }
      }

      char c1 = l1 == min ? '/' : o1.charAt(min);
      char c2 = l2 == min ? '/' : o2.charAt(min);

      int c = Character.compare(c1, c2);

      if(c != 0) {
        return c;
      }

      return l1 == l2 ? 0 : l1 < l2 ? -1 : 1;
    }
  };

  /*
   * Note on the sorting in the cache. The order in the cache should be such that all children
   * of a parent are directly below that parent. When doing a simplistic alphabetical sort
   * however the paths "a", "a/2" and "ab" are sorted as "a", "ab", "a/2". Appending a trailing
   * slash before sorting ("a/", "a/2/", "ab/") results in the desired order (same as input in
   * this case). The trailing character must be a slash or it won't sort correctly.
   */

  private final NavigableMap<String, Streamable> cache = new TreeMap<>(PATH_COMPARATOR);
  private final PersistentEventStream<StreamableEvent> persistentEventStream;
  private final Subscription cacheSubscription;

  @Inject
  public StreamableService(EventStore<StreamableEvent> store, Source<DiscoverEvent> eventSource) {
    this.persistentEventStream = new PersistentEventStream<>(store);

    cacheSubscription = persistentEventStream.plain().subscribe("StreamableService", e -> {
      URI location = e.location();

      if(e instanceof StreamableEvent.Updated u) {
        cache.put(location.toString(), u.streamable());
      }
      else if(e instanceof StreamableEvent.Removed r) {
        cache.remove(location.toString());
      }
    });

    eventSource.subscribe("StreamableService", this::process);
  }

  @Produces
  @Singleton
  EventSelector<StreamableEvent> events() {
    return persistentEventStream;
  }

  private void process(DiscoverEvent event) {
    cacheSubscription.join();

    String basePath = event.base().toString();
    List<Discovery> discoveries = event.discoveries().stream().sorted(Comparator.comparing(d -> d.location().toString(), PATH_COMPARATOR)).toList();
    List<Streamable> updatedStreamables = new ArrayList<>();
    List<String> removedItems = new ArrayList<>();
    Discovery currentDiscovery = null;
    int i = 0;

    outer:
    for(Entry<String, Streamable> entry : cache.tailMap(basePath, false).entrySet()) {
      Streamable cached = entry.getValue();
      String path = entry.getKey();

      if(!path.startsWith(basePath)) {
        break;
      }

      while(i < discoveries.size()) {
        Discovery d = discoveries.get(i);

        int c = PATH_COMPARATOR.compare(d.location().toString(), path);

        if(c > 0) {
          break;
        }

        currentDiscovery = d;

        i++;

        // Only add item if its parent exists in cache:
        if(d.parentLocation().map(Object::toString).map(cache::containsKey).orElse(true)) {
          Streamable streamable = create(d, event.identificationService(), event.tags());

          if(c < 0 || !streamable.equals(cached)) {
            updatedStreamables.add(streamable);
          }
        }

        if(c == 0) {
          continue outer;
        }
      }

      /*
       * Only remove existing item if its path does not start with the last seen
       * scraped path:
       */

      if(currentDiscovery == null || !path.startsWith(currentDiscovery.location().toString())) {
        removedItems.add(path);
      }
    }

    while(i < discoveries.size()) {
      Discovery item = discoveries.get(i++);

      // Only add item if its parent exists in cache:
      if(item.parentLocation().map(Object::toString).map(cache::containsKey).orElse(true)) {
        updatedStreamables.add(create(item, event.identificationService(), event.tags()));
      }
    }

    removedItems.stream().map(URI::create).map(StreamableEvent.Removed::new).forEach(persistentEventStream::push);
    updatedStreamables.stream().map(StreamableEvent.Updated::new).forEach(persistentEventStream::push);
  }

  private static Streamable create(Discovery discovery, Optional<String> identificationServiceName, StreamTags tags) {
    return new Streamable(discovery, identificationServiceName, tags);
  }
}

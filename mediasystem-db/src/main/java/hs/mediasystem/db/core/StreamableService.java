package hs.mediasystem.db.core;

import hs.ddif.annotations.Produces;
import hs.mediasystem.ext.basicmediatypes.api.DiscoverEvent;
import hs.mediasystem.ext.basicmediatypes.api.Discovery;
import hs.mediasystem.ext.basicmediatypes.api.StreamTags;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
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

@Singleton
public class StreamableService {
  private final NavigableMap<String, Streamable> cache = new TreeMap<>();
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
    List<Discovery> discoveries = event.discoveries().stream().sorted(Comparator.comparing(d -> d.location())).toList();
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

        int c = d.location().toString().compareTo(path);

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

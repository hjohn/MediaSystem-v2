package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;
import hs.mediasystem.db.core.StreamDescriptorFetchTaskManager.DescribedLocation;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.db.services.domain.ContentPrint;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.events.streams.Source;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Produces;

@Singleton
public class StreamableService {
  record Item(Streamable streamable, Discovery discovery, Optional<IdentificationProvider> identificationProvider) {}

  private static final Logger LOGGER = System.getLogger(StreamableService.class.getName());

  /*
   * Note on the sorting in the cache. The order in the cache should be such that all children
   * of a parent are directly below that parent. When doing a simplistic alphabetical sort
   * however the paths "a", "a/2" and "ab" are sorted as "a", "ab", "a/2". Appending a trailing
   * slash before sorting ("a/", "a/2/", "ab/") results in the desired order (same as input in
   * this case). The trailing character must be a slash or it won't sort correctly.
   */

  private final NavigableMap<String, Streamable> cache = new TreeMap<>(PATH_COMPARATOR);
  private final BlockingQueue<DescribedLocation> queue = new SynchronousQueue<>();
  private final Map<URI, Item> items = new HashMap<>();
  private final SynchronousEventStream<StreamableEvent> eventStream;  // TODO non-memory backed stream risks losing items before subscribers are present
  private final DatabaseContentPrintProvider contentPrintProvider;
  private final StreamDescriptorFetchTaskManager taskManager;

  @Inject
  public StreamableService(Source<DiscoverEvent> eventSource, DatabaseContentPrintProvider contentPrintProvider, StreamDescriptorService streamDescriptorService) {
    this.eventStream = new SynchronousEventStream<>();
    this.contentPrintProvider = contentPrintProvider;
    this.taskManager = new StreamDescriptorFetchTaskManager(streamDescriptorService, queue);

    eventSource.subscribe("StreamableService", this::process);

    Thread.ofPlatform()
      .daemon()
      .name(this.getClass().getSimpleName() + ":queueReader")
      .start(this::processQueue);
  }

  @Produces
  @Singleton
  EventStream<StreamableEvent> events() {
    return eventStream;
  }

  private synchronized void process(DiscoverEvent event) {
    String basePath = event.base().toString();
    List<Discovery> discoveries = event.discoveries().stream().sorted(Comparator.comparing(d -> d.location().toString(), PATH_COMPARATOR)).toList();
    List<StreamableEvent.Updated> updatedStreamables = new ArrayList<>();
    List<Streamable> removedItems = new ArrayList<>();
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
        if(event.parentLocation().map(Object::toString).map(cache::containsKey).orElse(true)) {
          Streamable streamable = create(d, event);

          if(streamable != null && (c < 0 || !streamable.equals(cached))) {
            updatedStreamables.add(new StreamableEvent.Updated(streamable, event.identificationProvider(), d));
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
        removedItems.add(cached);
      }
    }

    while(i < discoveries.size()) {
      Discovery d = discoveries.get(i++);

      // Only add item if its parent exists in cache:
      if(event.parentLocation().map(Object::toString).map(cache::containsKey).orElse(true)) {
        Streamable streamable = create(d, event);

        if(streamable != null) {
          updatedStreamables.add(new StreamableEvent.Updated(streamable, event.identificationProvider(), d));
        }
      }
    }

    removedItems.stream().map(Streamable::location).forEach(this::streamableRemoved);
    updatedStreamables.stream().forEach(this::streamableUpdated);
  }

  private synchronized void streamableUpdated(StreamableEvent.Updated updated) {
    items.put(updated.location(), new Item(updated.streamable(), updated.discovery(), updated.identificationProvider()));
    taskManager.create(updated.location());
    cache.put(updated.location().toString(), updated.streamable());
    eventStream.push(updated);
  }

  private synchronized void streamableRemoved(URI location) {
    items.remove(location);
    taskManager.stop(location);
    cache.remove(location.toString());
    eventStream.push(new StreamableEvent.Removed(location));
  }

  private void processQueue() {
    try {
      for(;;) {
        streamDescriptorUpdated(queue.take());
      }
    }
    catch(InterruptedException e) {
      LOGGER.log(Level.INFO, "Stopping " + Thread.currentThread());
    }
  }

  private synchronized void streamDescriptorUpdated(DescribedLocation result) {
    Item item = items.get(result.location());

    if(item != null) {
      eventStream.push(new StreamableEvent.Updated(
        item.streamable.with(result.descriptor()),
        item.identificationProvider,
        item.discovery
      ));
    }
  }

  private Streamable create(Discovery discovery, DiscoverEvent event) {
    try {
      ContentPrint contentPrint = contentPrintProvider.get(discovery.location(), discovery.size(), discovery.lastModificationTime());

      return new Streamable(discovery.mediaType(), discovery.location(), contentPrint, event.parentLocation(), event.tags(), Optional.empty());
    }
    catch(IOException e) {
      LOGGER.log(Level.WARNING, "Unable to create content hash for discovery, skipping: " + discovery + ", because: " + Throwables.formatAsOneLine(e));

      return null;
    }
  }

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
}

package hs.mediasystem.db.core;

import hs.ddif.annotations.Produces;
import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.db.DatabaseResponseCache.CacheMode;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.api.Discovery;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService;
import hs.mediasystem.ext.basicmediatypes.services.IdentificationService.Identification;
import hs.mediasystem.util.concurrent.NamedThreadFactory;
import hs.mediasystem.util.events.EventSelector;
import hs.mediasystem.util.events.PersistentEventStream;
import hs.mediasystem.util.events.store.EventStore;
import hs.mediasystem.util.events.streams.Subscription;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IdentifierService {
  private static final Logger LOGGER = Logger.getLogger(IdentifierService.class.getName());
  private static final Duration RETRY_ON_ERROR = Duration.ofHours(2);
  private static final Duration REFRESH = Duration.ofDays(14);  // refresh items periodically

  private final Map<String, IdentificationService> identificationServices;
  private final PersistentEventStream<IdentificationEvent> persistentEventStream;
  private final Map<URI, IdentificationEvent> parentDescriptors = new HashMap<>();
  private final DatabaseResponseCache responseCache;
  private final Subscription parentDescriptorCacheSubscription;
  private final BackgroundRefresher backgroundRefresher = new BackgroundRefresher();

  @Inject
  public IdentifierService(DatabaseResponseCache responseCache, EventStore<IdentificationEvent> eventStore, EventSelector<StreamableEvent> source, List<IdentificationService> identificationServices) {
    this.responseCache = responseCache;
    this.persistentEventStream = new PersistentEventStream<>(eventStore);
    this.identificationServices = identificationServices.stream().collect(Collectors.toMap(IdentificationService::getName, Function.identity()));

    if(identificationServices.stream().map(IdentificationService::getName).distinct().count() != identificationServices.size()) {
      throw new IllegalArgumentException("identificationServices cannot have services with the same name: " + identificationServices.stream().map(IdentificationService::getName).toList());
    }

    this.parentDescriptorCacheSubscription = persistentEventStream.plain().subscribe("IdentifierService", e -> {
      parentDescriptors.put(e.location(), e);
    });

    source.plain().subscribe("IdentifierService", this::process);
  }

  @Produces
  @Singleton
  public EventSelector<IdentificationEvent> identificationEvents() {
    return persistentEventStream;
  }

  public void reidentify(URI location) {
    backgroundRefresher.refreshNow(location);
  }

  private void process(StreamableEvent event) {
    // be careful not to throw exceptions from this method as it will break the event thread...
    responseCache.setCurrentThreadCacheMode(CacheMode.PREFER_CACHED);
    parentDescriptorCacheSubscription.join();  // ensure parents are in cache

    if(event instanceof StreamableEvent.Updated u && u.streamable().identificationService().isPresent()) {
      Streamable streamable = u.streamable();
      Discovery discovery = streamable.discovery();
      IdentificationEvent existingIdentification = parentDescriptors.get(u.location());
      String identificationServiceName = streamable.identificationService().orElseThrow();

      if(existingIdentification == null) {
        identify(identificationServiceName, discovery);
      }

      Duration timeSinceRefresh = existingIdentification == null ? Duration.ZERO : Duration.between(existingIdentification.match().creationTime(), Instant.now());

      backgroundRefresher.schedule(
        Instant.now().plusSeconds(Math.max(300, REFRESH.minus(timeSinceRefresh).toSeconds())),
        discovery,
        identificationServiceName
      );
    }
  }

  private void identify(String serviceName, Discovery discovery) {
    MediaType mediaType = discovery.mediaType();

    /*
     * Identification for dependent items like Episodes can only proceed when there
     * was a (successful) identification of its parent. For items in a hierarchy
     * (like files and folders) no parent identification is needed, nor is the parent
     * information passed to the identification service.
     */

    if(mediaType.isComponent()) {  // Must have a parent identification?
      WorkDescriptor parent = parentDescriptors.get(discovery.parentLocation().orElseThrow()).descriptors().get(0);  // TODO get(0) ain't nice, for parents it should be the only one

      if(parent == null) {
        LOGGER.warning("Identification skipped (missing parent identification) for: " + discovery.location());
      }
      else {
        identify(serviceName, discovery, parent);
      }
    }
    else {
      identify(serviceName, discovery, null);
    }
  }

  private void identify(String serviceName, Discovery discovery, WorkDescriptor parent) {
    URI location = discovery.location();

    if(serviceName == null) {
      LOGGER.info("Identification skipped (no service was configured) for: " + location);
    }
    else {
      IdentificationService service = identificationServices.get(serviceName);

      if(service == null) {
        LOGGER.info("Identification skipped (unknown service '" + service + "') for: " + location);
      }
      else {
        try {
          Identification identification = service.identify(discovery, parent).orElse(null);

          if(identification == null) {
            LOGGER.info("Identification with service '" + service + "' was not successful for: " + location);
          }
          else {
            persistentEventStream.push(new IdentificationEvent(location, identification.descriptors(), identification.match()));
          }
        }
        catch(IOException e) {
          LOGGER.warning("Identification with service '" + service + "' failed for: " + location + ", because: " + Throwables.formatAsOneLine(e));

          backgroundRefresher.schedule(
            Instant.now().plus(RETRY_ON_ERROR),
            discovery,
            serviceName
          );
        }
        catch(Exception e) {
          LOGGER.log(Level.SEVERE, "Identification with service '" + service + "' failed for: " + location, e);
        }
      }
    }
  }

  class BackgroundRefresher {
    private static final Comparator<Key> COMPARATOR = Comparator.comparing(Key::refreshTime).thenComparing(Key::uri, Comparator.nullsLast(Comparator.naturalOrder()));
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("IdentifierService:bg", true));

    private final NavigableMap<Key, Runnable> refreshes = new TreeMap<>(COMPARATOR);
    private final Map<URI, Key> keys = new HashMap<>();

    public BackgroundRefresher() {
      SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(this::doRefreshes, 5, 1, TimeUnit.MINUTES);
    }

    public void schedule(Instant refreshTime, Discovery discovery, String identificationService) {
      synchronized(refreshes) {

        // Remove old entry:
        Key existingKey = keys.remove(discovery.location());

        if(existingKey != null) {
          refreshes.remove(existingKey);
        }

        // Add new:
        Key key = new Key(refreshTime, discovery.location());

        keys.put(discovery.location(), key);
        refreshes.put(key, () -> identify(identificationService, discovery));
      }
    }

    public void refreshNow(URI location) {
      Runnable runnable;

      synchronized(refreshes) {
        Key key = keys.get(location);

        if(key == null) {
          return;
        }

        runnable = refreshes.get(key);
      }

      runnable.run();
    }

    private void doRefreshes() {
      Instant now = Instant.now();

      for(;;) {
        Entry<Key, Runnable> entry;

        synchronized(refreshes) {
          entry = refreshes.firstEntry();

          if(entry == null || !now.isAfter(entry.getKey().refreshTime())) {
            return;
          }

          Key newKey = new Key(now.plus(REFRESH), entry.getKey().uri());

          refreshes.remove(entry.getKey());
          refreshes.put(newKey, entry.getValue());
          keys.put(newKey.uri(), newKey);
        }

        entry.getValue().run();
      }
    }

    static record Key(Instant refreshTime, URI uri) {}
  }
}

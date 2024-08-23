package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.core.IdentificationTaskManager.IdentifiedLocation;
import hs.mediasystem.db.core.domain.Resource;
import hs.mediasystem.db.core.domain.Streamable;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.util.time.TimeSource;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.db.core.util.ThrowingSupplier;

@Singleton
public class ResourceService {
  private static final Logger LOGGER = System.getLogger(ResourceService.class.getName());

  /*
   * The following structures always contain an entry per known streamable:
   */

  private final Map<URI, Resource> resources = new HashMap<>();
  private final Map<URI, Streamable> streamables = new HashMap<>();
  private final Map<URI, Discovery> discoveries = new HashMap<>();

  /*
   * Only contains entry when background task has supplied information:
   */

  private final Map<URI, Identification> identifications = new HashMap<>();

  /*
   * Structures for faster querying:
   */

  private final Map<URI, Set<URI>> dependents = new HashMap<>();  // Root to Dependent (has no entry if a root has no dependents)
  private final Map<URI, URI> roots = new HashMap<>();  // Dependent to Root (not a parent child mapping, does not contain an entry for every resource!)
  private final Map<ContentID, Set<URI>> contentIds = new HashMap<>();

  private final IdentificationTaskManager identificationTaskManager;
  private final LinkedWorksService worksService;
  private final BlockingQueue<IdentifiedLocation> queue = new SynchronousQueue<>();
  private final ReentrantLock lock = new ReentrantLock();

  @Inject
  public ResourceService(IdentificationStore identificationStore, LinkedWorksService worksService) {
    this.worksService = worksService;
    this.identificationTaskManager = new IdentificationTaskManager(
      identificationStore,
      TimeSource.system(),
      Duration.ofDays(14),
      Duration.ofHours(2),
      queue
    );

    Thread.ofPlatform()
      .daemon()
      .name(this.getClass().getSimpleName() + ":queueReader")
      .start(this::processQueue);
  }

  /**
   * Returns the first resource matching the given {@link ContentID}.
   *
   * @param contentId a {@link ContentID}, cannot be {@code null}
   * @return an optional {@link Resource}, never {@code null}
   */
  public Optional<Resource> findFirst(ContentID contentId) {
    return doLocked(() -> contentIds.getOrDefault(contentId, Set.of()).stream().map(this::find).flatMap(Optional::stream).findFirst());
  }

  // Given a dependent, finds its root
  public Optional<Resource> findRoot(URI location) {
    return doLocked(() -> Optional.of(location).map(roots::get).map(resources::get));
  }

  public Optional<Resource> find(URI location) {
    return doLocked(() -> Optional.ofNullable(resources.get(location)));
  }

  public void reidentify(URI location) {
    lock.lock();

    try {
      URI rootLocation = roots.get(location);

      identificationTaskManager.reidentify(rootLocation == null ? location : rootLocation);
    }
    finally {
      lock.unlock();
    }
  }

  void handleEvent(StreamableEvent event) {
    lock.lock();

    try {
      whileLockedHandleEvent(event);
    }
    finally {
      lock.unlock();
    }
  }

  private void whileLockedHandleEvent(StreamableEvent event) {
    URI location = event.location();

    /*
     * Partially clean the given location from the tracking structures to
     * enable a clean update or a complete removal:
     */

    discoveries.remove(location);

    Streamable oldStreamable = streamables.remove(location);
    URI oldRoot = roots.remove(location);

    if(oldStreamable != null) {
      contentIds.computeIfPresent(oldStreamable.contentPrint().getId(), (k, v) -> v.remove(location) && v.isEmpty() ? null : v);
    }

    if(oldRoot != null) {
      dependents.computeIfPresent(oldRoot, (k, v) -> v.remove(location) && v.isEmpty() ? null : v);
    }

    /*
     * Event handling:
     */

    switch(event) {
      case StreamableEvent.Updated u -> {

        /*
         * Update main structures:
         */

        URI rootLocation = u.streamable().mediaType().isComponent()
          ? u.streamable().parentLocation().orElseThrow(() -> new IllegalStateException("Streamables that are components must have a parent: " + u.streamable()))
          : null;

        streamables.put(location, u.streamable());
        discoveries.put(location, u.discovery());
        contentIds.computeIfAbsent(u.streamable().contentId(), k -> new LinkedHashSet<>()).add(location);

        if(rootLocation != null) {
          roots.put(location, rootLocation);
          dependents.computeIfAbsent(rootLocation, k -> new HashSet<>()).add(location);
        }

        /*
         * Handle asynchronous identification:
         */

        IdentificationProvider provider = u.identificationProvider().orElse(null);

        if(provider != null) {
          if(rootLocation == null) {
            identificationTaskManager.create(provider, u.discovery());
          }
          else {
            Identification identification = identifications.get(rootLocation);

            if(identification != null) {
              identifications.put(location, provider.identifyChild(u.discovery(), identification));
            }
          }
        }

        /*
         * Create or Update resource and send out event:
         */

        updateResource(location);
      }
      case StreamableEvent.Removed r -> {

        /*
         * Stop asynchronous identification:
         */

        identifications.remove(location);

        if(oldRoot == null) {
          identificationTaskManager.stop(location);
        }

        /*
         * Remove resource and send out event:
         */

        removeResource(location);
      }
    }
  }

  private void processQueue() {
    try {
      for(;;) {
        identified(queue.take());
      }
    }
    catch(InterruptedException e) {
      LOGGER.log(Level.INFO, "Stopping " + Thread.currentThread());
    }
  }

  private void identified(IdentifiedLocation result) {
    lock.lock();

    try {
      URI rootLocation = result.location();
      Identification identification = result.identification();

      /*
       * As identifications are asynchronous, they could no longer be relevant.
       * In that case they should be ignored.
       */

      if(resources.containsKey(rootLocation)) {
        if(identification == null) {
          identifications.remove(rootLocation);
          updateResource(rootLocation);

          // Also remove all dependent identifications:
          for(URI dependent : dependents.getOrDefault(rootLocation, Set.of())) {
            identifications.remove(dependent);
            updateResource(dependent);
          }
        }
        else {
          identifications.put(rootLocation, identification);
          updateResource(rootLocation);

          // Also identify all dependents:
          for(URI dependent : dependents.getOrDefault(rootLocation, Set.of())) {
            identifications.put(dependent, result.provider().identifyChild(discoveries.get(dependent), identification));
            updateResource(dependent);
          }
        }
      }
    }
    finally {
      lock.unlock();
    }
  }

  private void updateResource(URI location) {
    Identification identification = getIdentification(location);
    Resource resource = new Resource(streamables.get(location), identification.match(), identification.releases());

    resources.put(location, resource);
    worksService.processEvent(new ResourceEvent.Updated(resource));
  }

  private void removeResource(URI location) {
    if(resources.remove(location) != null) {
      worksService.processEvent(new ResourceEvent.Removed(location));
    }
  }

  private Identification getIdentification(URI location) {
    Identification identification = identifications.get(location);

    if(identification != null) {
      return identification;
    }

    return IdentificationProvider.MINIMAL_PROVIDER.identify(discoveries.get(location)).orElseThrow(() -> new AssertionError("Minimal Provider should always have a result"));
  }

  private <T, X extends Exception> T doLocked(ThrowingSupplier<T, X> supplier) throws X {
    lock.lock();

    try {
      return supplier.get();
    }
    finally {
      lock.unlock();
    }
  }
}

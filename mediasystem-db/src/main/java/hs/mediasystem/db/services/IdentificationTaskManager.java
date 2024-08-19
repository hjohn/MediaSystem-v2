package hs.mediasystem.db.services;

import hs.mediasystem.api.datasource.domain.Identification;
import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.time.TimeSource;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.int4.db.core.util.ThrowingRunnable;
import org.int4.db.core.util.ThrowingSupplier;

/**
 * Runs tasks which do identification for root items, and calls back with
 * results.<p>
 *
 * Tasks will attempt to use an {@link IdentificationProvider} to do the
 * identification. Identification will be attempted again on errors, and will be
 * refreshed periodically when successful.<p>
 *
 * If at any time a previously identified item becomes unidentifiable (not due
 * to an error, but because the service can no longer match the item) then
 * the callback with be called with {@code null} as the identification result.
 */
class IdentificationTaskManager {

  /**
   * The identification status of a location.
   *
   * @param location the updated location, cannot be {@code null}
   * @param identification an identification, or {@code null} if unidentified
   * @param provider an {@link IdentificationProvider}, cannot be {@code null}
   */
  record IdentifiedLocation(URI location, Identification identification, IdentificationProvider provider) {}

  private static final Logger LOGGER = System.getLogger(IdentificationTaskManager.class.getName());

  private final Map<URI, Task> tasks = new HashMap<>();
  private final IdentificationStore identificationStore;
  private final TimeSource timeSource;
  private final Duration standardRefreshTime;
  private final Duration errorRefreshTime;
  private final BlockingQueue<IdentifiedLocation> queue;

  public IdentificationTaskManager(IdentificationStore identificationStore, TimeSource timeSource, Duration standardRefreshTime, Duration errorRefreshTime, BlockingQueue<IdentifiedLocation> queue) {
    this.identificationStore = identificationStore;
    this.timeSource = timeSource;
    this.standardRefreshTime = standardRefreshTime;
    this.errorRefreshTime = errorRefreshTime;
    this.queue = queue;
  }

  synchronized void create(IdentificationProvider provider, Discovery discovery) {
    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(discovery, "discovery");

    if(discovery.mediaType().isComponent()) {
      throw new IllegalArgumentException("dependent (component) types cannot be directly identified with this service: " + discovery);
    }

    Task oldTask = tasks.get(discovery.location());

    if(oldTask != null && oldTask.rootDiscovery.equals(discovery) && oldTask.externalIdentificationProvider.equals(provider)) {
      return;
    }

    tasks.put(discovery.location(), new Task(provider, discovery, false));

    if(oldTask != null) {
      oldTask.stop();
    }
  }

  /**
   * Stops the task, if any, for the given location.
   *
   * @param location a location, cannot be {@code null}
   */
  synchronized void stop(URI location) {
    stopInternal(location);
  }

  synchronized void reidentify(URI location) {
    Task oldTask = stopInternal(location);

    if(oldTask != null) {
      IdentificationProvider provider = oldTask.externalIdentificationProvider;
      Discovery discovery = oldTask.rootDiscovery;

      tasks.put(discovery.location(), new Task(provider, discovery, true));
    }
  }

  private Task stopInternal(URI location) {
    Task task = tasks.remove(location);

    if(task != null) {
      task.stop();

      return task;
    }

    return null;
  }

  private class Task implements Runnable {
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final Semaphore DATABASE_SEMAPHORE = new Semaphore(2, true);
    private static final Semaphore IDENTIFICATION_SEMAPHORE = new Semaphore(6, true);
    private static final Semaphore LOW_PRIORITY_IDENTIFICATION_SEMAPHORE = new Semaphore(3, true);

    private final IdentificationProvider externalIdentificationProvider;  // can be null
    private final URI rootLocation;
    private final Discovery rootDiscovery;
    private final Future<?> future;

    private Identification identification;  // null indicates there is no identification
    private IdentificationProvider activeProvider;  // indicates where last identification came from (null = there was none)

    private volatile boolean stopRequested;  // safety flag to see if thread interrupted flag is not subverted somewhere (like catching an InterruptedException but completely ignoring it)

    private boolean immediate;

    Task(IdentificationProvider identificationProvider, Discovery discovery, boolean immediate) {
      this.externalIdentificationProvider = identificationProvider;
      this.immediate = immediate;
      this.rootDiscovery = Objects.requireNonNull(discovery, "discovery");
      this.rootLocation = discovery.location();

      this.future = EXECUTOR.submit(this);
    }

    void stop() {  // Don't make this synchronized, as this task could be in the middle of a callback
      stopRequested = true;
      future.cancel(true);
    }

    private boolean hasIdentification() {
      return identification != null;
    }

    // When called with (null, null) the external provider did not produce an error but
    // specifically indicated the item was not identifiable at all (no match).
    private void update(Identification identification, IdentificationProvider usedProvider) throws InterruptedException {
      if(Objects.equals(this.identification, identification) && Objects.equals(this.activeProvider, usedProvider)) {
        return;
      }

      updateStatus("appending");

      this.identification = identification;
      this.activeProvider = usedProvider;

      queue.put(new IdentifiedLocation(rootLocation, identification, externalIdentificationProvider));
    }

    @Override
    public void run() {
      assert externalIdentificationProvider != null;

      /*
       * Infinite update task, continuously runs to update identification information periodically.
       */

      try {
        if(!immediate) {
          updateStatus("querying store");

          Identification identification = execute(() -> identificationStore.find(rootDiscovery.location()).orElse(null));

          if(identification != null) {
            // TODO database may be storing data provided by a DIFFERENT provider (if configuration was changed)
            update(identification, externalIdentificationProvider);

            sleepUntil(identification.match().creationTime().plus(standardRefreshTime));
          }
        }

        for(;;) {
          identify();
        }
      }
      catch(SQLException e) {
        LOGGER.log(Level.ERROR, "Database is required for background identification tasks to function: " + this, e);
      }
      catch(InterruptedException e) {
        LOGGER.log(Level.INFO, "Stopped successfully: " + this);
      }
      catch(Exception e) {
        LOGGER.log(Level.ERROR, "Exception running identification task: " + this, e);
      }
    }

    // TODO how to give immediate task temporarily a higher priority (so it won't get bogged down in dozens of queries to TMDB)

    private void identify() throws InterruptedException {
      URI location = rootDiscovery.location();

      try {
        updateStatus("querying provider " + externalIdentificationProvider.getName());

        Identification newIdentification = doIdentification();

        if(newIdentification == null) {
          update(null, null);
        }
        else {
          update(newIdentification, externalIdentificationProvider);
          updateStatus("updating store");
          execute(() -> identificationStore.store(location, newIdentification));
        }

        immediate = false;

        LOGGER.log(newIdentification == null ? Level.WARNING : Level.INFO, "Identification using " + externalIdentificationProvider.getName() + " for " + toLocationString() + (newIdentification == null ? " did not result in a match" : " was: " + newIdentification));

        sleepUntil(timeSource.instant().plus(standardRefreshTime));
      }
      catch(InterruptedException e) {
        throw e;
      }
      catch(IOException e) {
        LOGGER.log(Level.WARNING, "Identification with " + externalIdentificationProvider + " for " + toLocationString() + " was unsuccessful because: " + Throwables.formatAsOneLine(e));

        sleepUntil(timeSource.instant().plus(errorRefreshTime));
      }
      catch(Exception e) {
        LOGGER.log(Level.ERROR, "Identification with " + externalIdentificationProvider + " for " + toLocationString() + " failed with an exception", e);

        sleepUntil(timeSource.instant().plus(errorRefreshTime));
      }
    }

    private void sleepUntil(Instant instant) throws InterruptedException {
      updateStatus("sleeping until " + instant);
      timeSource.sleep(Duration.between(timeSource.instant(), instant));
    }

    private void updateStatus(String status) throws InterruptedException {
      checkInterrupt();

      Thread.currentThread().setName("IdTask(" + status + ")[" + toLocationString() + "]");
    }

    private Identification doIdentification() throws InterruptedException, IOException {
      boolean lowPriority = hasIdentification();

      if(lowPriority) {
        LOW_PRIORITY_IDENTIFICATION_SEMAPHORE.acquire();
      }

      try {
        IDENTIFICATION_SEMAPHORE.acquire();

        try {
          return externalIdentificationProvider.identify(rootDiscovery).orElse(null);
        }
        finally {
          IDENTIFICATION_SEMAPHORE.release();
        }
      }
      finally {
        if(lowPriority) {
          LOW_PRIORITY_IDENTIFICATION_SEMAPHORE.release();
        }
      }
    }

    private void checkInterrupt() throws InterruptedException {
      if(Thread.interrupted()) {
        throw new InterruptedException("Identification task was interrupted: " + toLocationString());
      }

      if(stopRequested) {
        LOGGER.log(Level.WARNING, "Interrupted status was not propagated correctly; stopping task based on secondary flag for: " + toLocationString());

        throw new InterruptedException("Identification task was interrupted: " + toLocationString());
      }
    }

    private <T> T execute(ThrowingSupplier<T, SQLException> supplier) throws SQLException, InterruptedException {
      int max = 5;

      for(int retry = 0; retry <= max; retry++) {
        try {
          DATABASE_SEMAPHORE.acquire();

          try {
            return supplier.get();
          }
          finally {
            DATABASE_SEMAPHORE.release();
          }
        }
        catch(InterruptedException e) {
          throw e;
        }
        catch(Exception e) {
          LOGGER.log(Level.WARNING, "Exception in SQL task attempt " + (retry + 1), e);

          Thread.sleep(Duration.ofMinutes(1));
        }
      }

      return supplier.get();  // Final attempt, whether successful or not...
    }

    private void execute(ThrowingRunnable<SQLException> runnable) throws SQLException, InterruptedException {
      execute(() -> {
        runnable.run();
        return null;
      });
    }

    private String toLocationString() {
      URI location = rootDiscovery.location();

      return "\"" + location.getScheme() + ":" + location.getSchemeSpecificPart() + "\"";
    }

    @Override
    public String toString() {
      return "Task[" + rootDiscovery + "]";
    }
  }
}
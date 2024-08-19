package hs.mediasystem.db.core;

import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.domain.media.StreamDescriptor;
import hs.mediasystem.util.bg.BackgroundTaskRegistry;
import hs.mediasystem.util.bg.BackgroundTaskRegistry.Workload;
import hs.mediasystem.util.concurrent.AutoSemaphore;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class StreamDescriptorFetchTaskManager {
  record DescribedLocation(URI location, StreamDescriptor descriptor) {}

  private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
  private static final Logger LOGGER = System.getLogger(StreamDescriptorFetchTaskManager.class.getName());
  private static final Workload WORKLOAD = BackgroundTaskRegistry.createWorkload("Extracting Metadata");

  private final StreamDescriptorService service;
  private final Map<URI, Task> tasks = new HashMap<>();
  private final BlockingQueue<DescribedLocation> queue;

  StreamDescriptorFetchTaskManager(StreamDescriptorService service, BlockingQueue<DescribedLocation> queue) {
    this.service = service;
    this.queue = queue;
  }

  synchronized void create(URI location) {
    // Always create a new task, as the old task may have indexed an old file (at the same location) with different content
    // The stream descriptor service is smart enough to provide the data from cache if this was not the case.
    stop(location);

    tasks.put(location, new Task(location));
  }

  synchronized void stop(URI location) {
    Task task = tasks.remove(location);

    if(task != null) {
      task.stop();
    }
  }

  private class Task implements Runnable {
    private static final AutoSemaphore FAST = new AutoSemaphore(5);
    private static final AutoSemaphore SLOW = new AutoSemaphore(1);

    private final URI location;
    private final Future<?> future;

    private volatile boolean stopRequested;  // safety flag to see if thread interrupted flag is not subverted somewhere (like catching an InterruptedException but completely ignoring it)

    Task(URI location) {
      this.location = location;
      this.future = EXECUTOR.submit(this);
    }

    void stop() {  // Don't make this synchronized, as this task could be in the middle of a callback
      stopRequested = true;
      future.cancel(true);
    }

    @Override
    public void run() {
      WORKLOAD.start();

      try {
        StreamDescriptor descriptor = FAST.execute(() -> service.get(location).orElse(null));

        checkInterrupt();

        if(descriptor == null) {
          descriptor = SLOW.execute(() -> service.create(location).orElse(null));
        }

        checkInterrupt();

        if(descriptor != null) {
          queue.put(new DescribedLocation(location, descriptor));
        }
      }
      catch(SQLException e) {
        LOGGER.log(Level.ERROR, "Database is required for background describe tasks to function: " + this, e);
      }
      catch(InterruptedException e) {
        LOGGER.log(Level.INFO, "Stopped successfully: " + this);
      }
      catch(Exception e) {
        LOGGER.log(Level.ERROR, "Exception running identification task: " + this, e);
      }
      finally {
        // If the task ended on its own, remove it from the tracking structure:
        StreamDescriptorFetchTaskManager.this.stop(location);

        WORKLOAD.complete();
      }
    }

    @Override
    public String toString() {
      return "StreamDescriptorTask[" + toLocationString() + "]";
    }

    private String toLocationString() {
      return "\"" + location.getScheme() + ":" + location.getSchemeSpecificPart() + "\"";
    }

    private void checkInterrupt() throws InterruptedException {
      if(Thread.interrupted()) {
        throw new InterruptedException(toString());
      }

      if(stopRequested) {
        LOGGER.log(Level.WARNING, "Interrupted status was not propagated correctly; stopping task based on secondary flag: " + this);

        throw new InterruptedException(toString());
      }
    }
  }
}
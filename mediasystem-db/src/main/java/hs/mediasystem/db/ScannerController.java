package hs.mediasystem.db;

import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.util.AutoReentrantLock;
import hs.mediasystem.util.AutoReentrantLock.Key;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ScannerController {
  private static final ScheduledThreadPoolExecutor EXECUTOR;
  private static final Logger LOGGER = Logger.getLogger(ScannerController.class.getName());

  static {
    EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ScannerController"));
    EXECUTOR.setRemoveOnCancelPolicy(true);
  }

  @Inject private MediaManagerUpdater mediaManagerUpdater;

  private final AutoReentrantLock mediaManagerLock = new AutoReentrantLock();

  private AtomicBoolean cancelled;
  private ScheduledFuture<?> future;

  // Run scans periodically, taking care to not overlap scans
  // Scan results should be added to db

  public void setSuppliers(List<ScanResultSupplier> suppliers) {
    if(future != null) {
      cancelled.set(true);
      future.cancel(true);
    }

    mediaManagerUpdater.setScannerIds(suppliers.stream().collect(Collectors.toMap(ScanResultSupplier::getId, ScanResultSupplier::getStreamSource)));

    // TODO when scanners are reconfigured, we may need to purge things from MediaManager...

    List<ScanResultSupplier> copiedSuppliers = new ArrayList<>(suppliers);

    try(Key key = mediaManagerLock.lock()) {  // Grabbing the lock will ensure that any old tasks aren't midway in an update and have been properly cancelled
      cancelled = new AtomicBoolean();
      future = EXECUTOR.scheduleWithFixedDelay(() -> scanAll(copiedSuppliers, cancelled), 0, 5, TimeUnit.MINUTES);
    }
  }

  /**
   * Executes scans for all the given Scanners.  If cancelled, exit early and donot report any results.
   *
   * @param suppliers a list of {@link Supplier}s
   * @param cancelled whether or not this task was cancelled
   */
  private void scanAll(List<ScanResultSupplier> suppliers, AtomicBoolean cancelled) {
    LOGGER.info("Initiating scan with " + suppliers.size() + " scanners...");

    for(ScanResultSupplier supplier : suppliers) {
      try {
        List<Exceptional<List<BasicStream>>> results = supplier.scan();

        try(Key key = mediaManagerLock.lock()) {
          if(cancelled.get()) {
            return;
          }

          for(int i = 0; i < results.size(); i++) {
            Exceptional<List<BasicStream>> result = results.get(i);

            if(result.isException()) {
              LOGGER.warning(supplier.getName() + " failed while scanning '" + supplier.getRoots().get(i) + "' with: " + result.getException());
            }
            else {
              LOGGER.fine(supplier.getName() + " returned " + result.get().size() + " items while scanning '" + supplier.getRoots().get(i) + "'");
            }
          }

          mediaManagerUpdater.update(supplier.getId(), supplier.getStreamSource(), results);
        }
      }
      catch(Throwable t) {
        LOGGER.severe("Exception while running Scanner: " + supplier.getName() + ": " + Throwables.formatAsOneLine(t));
      }
    }
  }
}

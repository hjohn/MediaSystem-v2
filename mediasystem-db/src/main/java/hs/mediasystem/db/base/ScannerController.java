package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.Exceptional;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ScannerController {
  private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ScannerController"));
  private static final Logger LOGGER = Logger.getLogger(ScannerController.class.getName());

  @Inject private StreamCacheUpdateService updateService;
  @Inject private ImportSourceProvider importSourceProvider;
  @Inject @Nullable @Named("scanners.initialDelay") private Long initialDelay = 15L;  // After 15 seconds start scans
  @Inject @Nullable @Named("scanners.delay") private Long delay = 5 * 60L;  // Time in between scans: 5 minutes

  @PostConstruct
  private void postConstruct() {
    EXECUTOR.scheduleWithFixedDelay(this::scanAll, initialDelay, delay, TimeUnit.SECONDS);
  }

  public void scanNow() {
    EXECUTOR.execute(this::scanAll);
  }

  /**
   * Executes scans for all current import sources.
   */
  private synchronized void scanAll() {
    List<ImportSource> sources = importSourceProvider.getImportSources();

    LOGGER.info("Initiating scan with " + sources.size() + " scanners...");

    for(ImportSource source : sources) {
      String scannerName = source.getScanner().getClass().getSimpleName();

      try {
        List<Exceptional<List<Streamable>>> results = source.getScanner().scan(source.getRoots());

        for(int i = 0; i < results.size(); i++) {
          Exceptional<List<Streamable>> result = results.get(i);

          if(result.isException()) {
            LOGGER.warning(scannerName + " failed while scanning '" + source.getRoots().get(i) + "' with: " + result.getException());
          }
          else {
            LOGGER.fine(scannerName + " returned " + result.get().size() + " items while scanning '" + source.getRoots().get(i) + "'");
          }
        }

        updateService.update(source.getId(), results);
      }
      catch(Throwable t) {
        LOGGER.severe("Exception while running Scanner: " + scannerName + ": " + Throwables.formatAsOneLine(t));
      }
    }
  }
}

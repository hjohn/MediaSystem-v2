package hs.mediasystem.db.core;

import hs.ddif.annotations.Opt;
import hs.ddif.annotations.Produces;
import hs.mediasystem.ext.basicmediatypes.api.DiscoverEvent;
import hs.mediasystem.ext.basicmediatypes.api.ImportSource;
import hs.mediasystem.util.NamedThreadFactory;
import hs.mediasystem.util.Throwables;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.Source;
import hs.mediasystem.util.events.streams.EventStream;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class DiscoveryController {
  private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("DiscoveryController"));
  private static final Logger LOGGER = Logger.getLogger(DiscoveryController.class.getName());

  private final EventStream<DiscoverEvent> eventStream = new SynchronousEventStream<>();

  @Inject private Collection<ImportSource> importSources;
  @Inject @Opt @Named("server.discovery.initial-delay") private Long initialDelay = 15L;  // After 15 seconds start scans
  @Inject @Opt @Named("server.discovery.delay") private Long delay = 5 * 60L;  // Time in between scans: 5 minutes

  @PostConstruct
  private void postConstruct() {
    EXECUTOR.scheduleWithFixedDelay(this::discoverAll, initialDelay, delay, TimeUnit.SECONDS);
  }

  @Produces
  Source<DiscoverEvent> discoverEvents() {
    return eventStream;
  }

  /**
   * Executes scans for all current import sources.
   */
  private synchronized void discoverAll() {
    LOGGER.info("Initiating discovery with " + importSources.size() + " sources...");

    for(ImportSource source : importSources) {
      try {
        source.discoverer().discover(source.root(), (uri, discoveries) -> eventStream.push(new DiscoverEvent(uri, source.identificationService(), source.tags(), discoveries)));
      }
      catch(IOException e) {
        LOGGER.warning("Failed scanning: " + source + "; exception: " + Throwables.formatAsOneLine(e));
      }
    }
  }
}
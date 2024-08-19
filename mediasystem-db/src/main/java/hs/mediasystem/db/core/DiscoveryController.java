package hs.mediasystem.db.core;

import hs.mediasystem.api.datasource.services.IdentificationProvider;
import hs.mediasystem.api.discovery.Discoverer;
import hs.mediasystem.util.concurrent.NamedThreadFactory;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.streams.EventStream;
import hs.mediasystem.util.events.streams.Source;
import hs.mediasystem.util.exception.Throwables;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.annotations.Produces;

@Singleton
public class DiscoveryController {
  private static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("DiscoveryController"));
  private static final Logger LOGGER = Logger.getLogger(DiscoveryController.class.getName());

  private final EventStream<DiscoverEvent> eventStream = new SynchronousEventStream<>();

  @Inject private Collection<ImportSource> importSources;
  @Inject private List<IdentificationProvider> identificationProviders;
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
      LOGGER.info("Discovering " + source + "...");

      IdentificationProvider provider = identificationProviders.stream()
        .filter(ip -> ip.getName().equals(source.identificationService().orElse(null)))
        .findFirst()
        .orElse(null);

      try {
        Discoverer.Registry registry = (parentLocation, discoveries) -> eventStream.push(new DiscoverEvent(
          parentLocation == null ? source.root() : parentLocation,
          Optional.ofNullable(provider),
          source.tags(),
          parentLocation == null || parentLocation.equals(source.root()) ? Optional.empty() : Optional.of(parentLocation),
          discoveries
        ));

        source.discoverer().discover(source.root(), registry);
      }
      catch(IOException e) {
        LOGGER.warning("Failed scanning: " + source + "; exception: " + Throwables.formatAsOneLine(e));
      }
    }
  }
}

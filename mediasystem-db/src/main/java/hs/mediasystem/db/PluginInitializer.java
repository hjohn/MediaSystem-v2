package hs.mediasystem.db;

import hs.mediasystem.db.services.collection.CollectionLocationManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.int4.dirk.api.CandidateRegistry;
import org.int4.dirk.api.InstanceResolver;
import org.int4.dirk.plugins.ComponentScannerFactory;
import org.int4.dirk.plugins.Plugin;
import org.int4.dirk.plugins.PluginManager;

/**
 * Configures and runs the back-end services.
 */
public class PluginInitializer {
  private static final Logger LOGGER = Logger.getLogger(PluginInitializer.class.getName());

  @Inject private InstanceResolver instanceResolver;
  @Inject private CandidateRegistry store;
  @Inject private ComponentScannerFactory componentScannerFactory;
  @Inject @Nullable @Named("general.basedir") private String baseDir = ".";

  @PostConstruct
  private void postConstruct() throws IOException {
    PluginManager pluginManager = new PluginManager(componentScannerFactory, store);
    List<Plugin> plugins = new ArrayList<>();
    Path root = Paths.get(baseDir, "plugins");

    if(Files.exists(root)) {
      LOGGER.fine("Loading plugins from: " + root);

      try(Stream<Path> roots = Files.find(root, 1, (p, a) -> !p.equals(root))) {
        roots.forEach(p -> {
          try(Stream<Path> stream = Files.find(p, 1, (cp, a) -> !cp.equals(p))) {
            List<URL> urls = stream.map(Path::toUri).map(uri -> {
              try {
                LOGGER.fine("Found plugin: " + uri);

                return uri.toURL();
              }
              catch(MalformedURLException e) {
                throw new IllegalStateException(e);
              }
            }).collect(Collectors.toList());

            if(!urls.isEmpty()) {
              plugins.add(pluginManager.loadPluginAndScan(urls.toArray(new URL[] {})));
            }
          }
          catch(IOException e) {
            throw new IllegalStateException(e);
          }
        });
      }
    }
    else {
      loadDevPlugin(pluginManager, "scanners").ifPresent(plugins::add);
      loadDevPlugin(pluginManager, "tmdb").ifPresent(plugins::add);
      loadDevPlugin(pluginManager, "local").ifPresent(plugins::add);
    }

    LOGGER.info(plugins.size() + " plugins loaded from: " + root);

    instanceResolver.getInstance(CollectionLocationManager.class);  // Triggers parsing of yaml's
  }

  private static Optional<Plugin> loadDevPlugin(PluginManager pluginManager, String name) throws MalformedURLException {
    Path base = Path.of("../mediasystem-ext-" + name + "/target/");

    if(Files.isDirectory(base)) {
      LOGGER.info("Found " + name + " plugin in development environment");

      return Optional.of(pluginManager.loadPluginAndScan(
        base.resolve("classes/").toUri().toURL(),
        base.resolve("dependencies-only.jar").toUri().toURL()
      ));
    }

    return Optional.empty();
  }
}

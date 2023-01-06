package hs.mediasystem.db;

import hs.ddif.core.api.InstanceResolver;
import hs.ddif.core.config.standard.InjectableStoreCandidateRegistry;
import hs.ddif.plugins.ComponentScannerFactory;
import hs.ddif.plugins.Plugin;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.db.extract.MediaMetaDataExtractor;
import hs.mediasystem.db.services.collection.CollectionLocationManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Configures and runs the back-end services.
 */
public class PluginInitializer {
  private static final Logger LOGGER = Logger.getLogger(PluginInitializer.class.getName());

  @Inject private InstanceResolver instanceResolver;
  @Inject private InjectableStoreCandidateRegistry store;
  @Inject private ComponentScannerFactory componentScannerFactory;
  @Inject @Nullable @Named("general.basedir") private String baseDir = ".";

  @PostConstruct
  private void postConstruct() throws IOException {
    PluginManager pluginManager = new PluginManager(componentScannerFactory, store);
    List<Plugin> plugins = new ArrayList<>();
    Path root = Paths.get(baseDir, "plugins");

    if(Files.exists(root)) {
      LOGGER.fine("Loading plugins from: " + root);

      Files.find(root, 1, (p, a) -> !p.equals(root)).forEach(p -> {
        try {
          List<URL> urls = Files.find(p, 1, (cp, a) -> !cp.equals(p)).map(Path::toUri).map(uri -> {
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
    else {
      plugins.add(pluginManager.loadPluginAndScan(URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-scanners/target/classes/").toURL()));
      plugins.add(pluginManager.loadPluginAndScan(URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-tmdb/target/classes/").toURL()));
      plugins.add(pluginManager.loadPluginAndScan(URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-local/target/classes/").toURL()));
    }

    LOGGER.info(plugins.size() + " plugins loaded from: " + root);

    instanceResolver.getInstance(CollectionLocationManager.class);  // Triggers parsing of yaml's
    instanceResolver.getInstance(MediaMetaDataExtractor.class);  // Triggers background thread
  }
}

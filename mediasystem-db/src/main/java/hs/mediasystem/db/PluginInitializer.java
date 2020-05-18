package hs.mediasystem.db;

import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.Plugin;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.db.base.ScannerController;
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

  @Inject private Instantiator instantiator;
  @Inject private BeanDefinitionStore store;
  @Inject @Nullable @Named("general.basedir") private String baseDir = ".";

  @PostConstruct
  private void postConstruct() throws IOException, BeanResolutionException {
    PluginManager pluginManager = new PluginManager(store);
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

    instantiator.getInstance(CollectionLocationManager.class);  // Triggers parsing of yaml's
    instantiator.getInstance(ScannerController.class);       // Triggers background thread
    instantiator.getInstance(MediaMetaDataExtractor.class);  // Triggers background thread
  }
}

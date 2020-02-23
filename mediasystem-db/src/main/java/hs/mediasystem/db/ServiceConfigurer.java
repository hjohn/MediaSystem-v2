package hs.mediasystem.db;

import hs.database.schema.DatabaseUpdater;
import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.db.base.ScannerController;
import hs.mediasystem.db.extract.MediaMetaDataExtractor;
import hs.mediasystem.db.services.collection.CollectionLocationManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class ServiceConfigurer {
  private static final Logger LOGGER = Logger.getLogger(CollectionLocationManager.class.getName());

  @Inject private Instantiator instantiator;
  @Inject private BeanDefinitionStore store;
  @Inject @Nullable @Named("general.basedir") private String baseDir = ".";

  @PostConstruct
  private void postConstruct() throws IOException, BeanResolutionException {
    PluginManager pluginManager = new PluginManager(store);

    pluginManager.loadPluginAndScan("hs.mediasystem.db", "hs.mediasystem.mediamanager");

    DatabaseUpdater updater = instantiator.getInstance(DatabaseUpdater.class);

    updater.updateDatabase("db-scripts");

    configureResponseCache(instantiator);

    Path root = Paths.get(baseDir, "plugins");

    if(Files.exists(root)) {
      Files.find(root, 1, (p, a) -> !p.equals(root)).forEach(p -> {
        try {
          List<URL> urls = Files.find(p, 1, (cp, a) -> !cp.equals(p)).map(Path::toUri).map(uri -> {
            try {
              return uri.toURL();
            }
            catch(MalformedURLException e) {
              throw new IllegalStateException(e);
            }
          }).collect(Collectors.toList());

          pluginManager.loadPluginAndScan(urls.toArray(new URL[] {}));
        }
        catch(IOException e) {
          throw new IllegalStateException(e);
        }
      });
    }
    else {
      pluginManager.loadPluginAndScan(URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-scanners/target/classes/").toURL());
      pluginManager.loadPluginAndScan(URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-tmdb/target/classes/").toURL());
      pluginManager.loadPluginAndScan(URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-local/target/classes/").toURL());
    }

    LOGGER.info("Plugins loaded from " + root);

    instantiator.getInstance(CollectionLocationManager.class);  // Triggers parsing of yaml's
    instantiator.getInstance(ScannerController.class);       // Triggers background thread
    instantiator.getInstance(MediaMetaDataExtractor.class);  // Triggers background thread
  }

  private static void configureResponseCache(Instantiator instantiator) throws BeanResolutionException {
    ResponseCache.setDefault(instantiator.getInstance(DatabaseResponseCache.class));
  }
}

package hs.mediasystem.db;

import hs.database.schema.DatabaseUpdater;
import hs.ddif.core.Injector;
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
import java.util.stream.Collectors;

/**
 * Configures and runs the back-end services.
 */
public class ServiceConfigurer {

  public static Injector configure(Injector injector) throws SecurityException, IOException {
    PluginManager pluginManager = new PluginManager(injector);

    pluginManager.loadPluginAndScan("hs.mediasystem.db", "hs.mediasystem.mediamanager");

    DatabaseUpdater updater = injector.getInstance(DatabaseUpdater.class);

    updater.updateDatabase("db-scripts");

    configureResponseCache(injector);

    Path root = Paths.get("plugins");

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

    injector.getInstance(CollectionLocationManager.class);  // Triggers parsing of yaml's
    injector.getInstance(ScannerController.class);       // Triggers background thread
    injector.getInstance(MediaMetaDataExtractor.class);  // Triggers background thread

    return injector;
  }

  private static void configureResponseCache(Injector injector) {
    ResponseCache.setDefault(injector.getInstance(DatabaseResponseCache.class));
  }
}

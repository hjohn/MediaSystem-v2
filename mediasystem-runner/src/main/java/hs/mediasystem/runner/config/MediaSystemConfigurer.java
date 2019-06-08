package hs.mediasystem.runner.config;

import hs.ddif.core.Injector;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.db.DatabaseStreamPrintProvider;
import hs.mediasystem.db.ScannerController;
import hs.mediasystem.db.extract.MediaMetaDataExtractor;
import hs.mediasystem.domain.PlayerFactory;
import hs.mediasystem.runner.CollectionLocationManager;
import hs.mediasystem.runner.expose.Annotations;
import hs.mediasystem.runner.util.DatabaseResponseCache;
import hs.mediasystem.util.ini.Ini;

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

public class MediaSystemConfigurer {

  public static Injector configure(Injector injector) throws SecurityException, IOException {
    configureResponseCache(injector);

    injector.register(DatabaseStreamPrintProvider.class);

    PluginManager pluginManager = new PluginManager(injector);
    Path root = Paths.get("plugins");

    pluginManager.loadPluginAndScan("hs.mediasystem.db", "hs.mediasystem.mediamanager");

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
      pluginManager.loadPluginAndScan(
        URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/classes/").toURL(),
        new URL("file:P:/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/dependencies-only.jar")
      );
    }

    injector.getInstance(CollectionLocationManager.class);  // Triggers parsing of yaml's
    injector.getInstance(ScannerController.class);       // Triggers background thread
    injector.getInstance(MediaMetaDataExtractor.class);  // Triggers background thread

//    try {
//      p.getClassLoader().loadClass("com.sun.jna.NativeLibrary");
//    }
//    catch(ClassNotFoundException e) {
//      throw new RuntimeException(e);
//    }
    injector.getInstance(PlayerFactory.class).create(injector.getInstance(Ini.class));

    Annotations.initialize();

    return injector;
  }

  private static void configureResponseCache(Injector injector) {
    ResponseCache.setDefault(injector.getInstance(DatabaseResponseCache.class));
  }
}

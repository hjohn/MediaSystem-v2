package hs.mediasystem.runner;

import hs.database.core.ConnectionPool;
import hs.database.core.SimpleConnectionPoolDataSource;
import hs.database.core.SimpleDatabaseStatementTranslator;
import hs.database.schema.DatabaseStatementTranslator;
import hs.database.schema.DatabaseUpdater;
import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.plugins.Plugin;
import hs.ddif.plugins.PluginManager;
import hs.mediasystem.db.DatabaseStreamPrintProvider;
import hs.mediasystem.db.ScannerController;
import hs.mediasystem.db.extract.MediaMetaDataExtractor;
import hs.mediasystem.domain.PlayerFactory;
import hs.mediasystem.runner.expose.Annotations;
import hs.mediasystem.runner.util.DatabaseResponseCache;
import hs.mediasystem.util.ini.Ini;
import hs.mediasystem.util.ini.Section;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Provider;
import javax.sql.ConnectionPoolDataSource;

public class MediaSystemConfigurer {
  private static final Logger LOGGER = Logger.getLogger(MediaSystemConfigurer.class.getName());

  public static Injector start() throws SecurityException, IOException {
    configureLogging();

    LOGGER.info("Java version: " + System.getProperty("java.version"));

    Injector injector = createInjector();

    Ini ini = createIni();

    injector.registerInstance(ini);

    configureDatabase(injector, ini.getSection("database"));
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
      Plugin p = pluginManager.loadPluginAndScan(
        URI.create("file://P/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/classes/").toURL(),
        new URL("file:P:/Dev/git/MediaSystem-v2/mediasystem-ext-vlc/target/dependencies-only.jar")
  //      URI.create("file://P/Dev/Workspaces/workspace-java8/mediasystem-ext-vlc/target/dependencies-only.jar").toURL()
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
    injector.getInstance(PlayerFactory.class).create(ini);

    Annotations.initialize();

    return injector;
  }

  private static void configureLogging() throws SecurityException, IOException {
    try(FileInputStream stream = new FileInputStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(stream);
    }
    catch(FileNotFoundException e) {
      System.out.println("[INFO] File 'logging.properties' not found, using defaults");
    }

    LOGGER.info("Logging configured");
  }

  private static void configureResponseCache(Injector injector) {
    ResponseCache.setDefault(injector.getInstance(DatabaseResponseCache.class));
  }

  private static Injector createInjector() {
    return new Injector(new JustInTimeDiscoveryPolicy());
  }

  private static Ini createIni() {
    return new Ini(new File("mediasystem.ini"));
  }

  private static void configureDatabase(Injector injector, Section databaseIniSection) {
    if(databaseIniSection == null) {
      try {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
      }
      catch(ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }

    ConnectionPoolDataSource dataSource = databaseIniSection == null ? new SimpleConnectionPoolDataSource("jdbc:derby:db;create=true") : configureDataSource(databaseIniSection);
    String databaseUrl = databaseIniSection == null ? "jdbc:derby:db;create=true" : databaseIniSection.get("url");

    LOGGER.info("Using database URL: " + databaseUrl);

    DatabaseStatementTranslator translator = createDatabaseStatementTranslator(databaseUrl);

    injector.register(new Provider<Connection>() {
      private final ConnectionPool pool = new ConnectionPool(dataSource, 5);

      @Override
      public Connection get() {
        return pool.getConnection();
      }
    });

    injector.register(new Provider<DatabaseStatementTranslator>() {
      @Override
      public DatabaseStatementTranslator get() {
        return translator;
      }
    });

    DatabaseUpdater updater = injector.getInstance(DatabaseUpdater.class);

    updater.updateDatabase("db-scripts");
  }

  private static DatabaseStatementTranslator createDatabaseStatementTranslator(String url) {
    String databaseName = url.split(":")[1].toLowerCase();

    if(databaseName.equals("postgresql")) {
      return new SimpleDatabaseStatementTranslator(new HashMap<String, String>() {{
        put("BinaryType", "bytea");
        put("DropNotNull", "DROP NOT NULL");
        put("Sha256Type", "bytea");
        put("SerialType", "serial4");
      }});
    }

    return new SimpleDatabaseStatementTranslator(new HashMap<String, String>() {{
      put("BinaryType", "blob");
      put("DropNotNull", "NULL");
      put("Sha256Type", "char(32) for bit data");
      put("SerialType", "integer generated always as identity");
    }});
  }

  private static ConnectionPoolDataSource configureDataSource(Section section) {
    try {
      Class.forName(section.get("driverClass"));
      Properties properties = new Properties();

      for(String key : section) {
        if(!key.equals("driverClass") && !key.equals("postConnectSql") && !key.equals("url")) {
          properties.put(key, section.get(key));
        }
      }

      SimpleConnectionPoolDataSource dataSource = new SimpleConnectionPoolDataSource(section.get("url"), properties);

      dataSource.setPostConnectSql(section.get("postConnectSql"));

      return dataSource;
    }
    catch(ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}

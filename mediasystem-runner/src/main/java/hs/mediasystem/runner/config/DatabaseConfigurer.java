package hs.mediasystem.runner.config;

import hs.database.core.ConnectionPool;
import hs.database.core.SimpleConnectionPoolDataSource;
import hs.database.core.SimpleDatabaseStatementTranslator;
import hs.database.schema.DatabaseStatementTranslator;
import hs.database.schema.DatabaseUpdater;
import hs.ddif.core.Injector;
import hs.mediasystem.util.ini.Section;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;

import javax.inject.Provider;
import javax.sql.ConnectionPoolDataSource;

public class DatabaseConfigurer {
  private static final Logger LOGGER = Logger.getLogger(DatabaseConfigurer.class.getName());

  public static void configure(Injector injector, Section databaseIniSection) {
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

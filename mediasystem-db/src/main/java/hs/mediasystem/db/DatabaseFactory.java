package hs.mediasystem.db;

import hs.database.core.ConnectionPool;
import hs.database.core.Database;
import hs.database.core.SimpleConnectionPoolDataSource;
import hs.database.core.SimpleDatabaseStatementTranslator;
import hs.database.schema.DatabaseStatementTranslator;
import hs.database.schema.DatabaseUpdater;
import hs.ddif.core.Produces;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.ConnectionPoolDataSource;

@Singleton
public class DatabaseFactory {
  private static final Logger LOGGER = Logger.getLogger(DatabaseFactory.class.getName());

  @Inject @Nullable @Named("database.driverClass") private String driverClass;
  @Inject @Nullable @Named("database.url") private String url;
  @Inject @Nullable @Named("database.user") private String user;
  @Inject @Nullable @Named("database.password") private String password;
  @Inject @Nullable @Named("database.postConnectSql") private String postConnectSql;

  private ConnectionPool pool;

  @PostConstruct
  private void postConstruct() {
    ConnectionPoolDataSource dataSource = url == null ? createDerbyDataSource() : createUserDataSource();

    pool = new ConnectionPool(dataSource, 5);
  }

  @Produces
  Connection createConnection() {
    return pool.getConnection();
  }

  @Produces
  @Singleton
  Database createDatabase(DatabaseStatementTranslator translator, Provider<Connection> connectionProvider) {
    new DatabaseUpdater(connectionProvider, translator).updateDatabase("db-scripts");

    return new Database(connectionProvider);
  }

  @Produces
  @Singleton
  DatabaseStatementTranslator createTranslator() {
    String databaseName = url == null ? "derby" : url.split(":")[1].toLowerCase();

    LOGGER.info(url == null ? "Using embedded Derby database" : "Using database URL: " + url);

    Map<String, String> translations;

    if(databaseName.equals("postgresql")) {
      translations = Map.of(
        "BinaryType", "bytea",
        "DropNotNull", "DROP NOT NULL",
        "Sha256Type", "bytea",
        "SerialType", "serial4"
      );
    }
    else {
      translations = Map.of(
        "BinaryType", "blob",
        "DropNotNull", "NULL",
        "Sha256Type", "char(32) for bit data",
        "SerialType", "integer generated always as identity"
      );
    }

    return new SimpleDatabaseStatementTranslator(translations);
  }

  private static ConnectionPoolDataSource createDerbyDataSource() {
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

      return new SimpleConnectionPoolDataSource("jdbc:derby:db;create=true");
    }
    catch(ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private ConnectionPoolDataSource createUserDataSource() {
    try {
      Class.forName(driverClass);
      Properties properties = new Properties();

      if(user != null) {
        properties.put("user", user);
      }
      if(password != null) {
        properties.put("password", password);
      }

      SimpleConnectionPoolDataSource dataSource = new SimpleConnectionPoolDataSource(url, properties);

      dataSource.setPostConnectSql(postConnectSql);

      return dataSource;
    }
    catch(ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}

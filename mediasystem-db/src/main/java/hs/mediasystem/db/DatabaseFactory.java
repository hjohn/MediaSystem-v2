package hs.mediasystem.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.int4.db.core.DatabaseBuilder;
import org.int4.db.core.api.CheckedDatabase;
import org.int4.db.core.api.Database;
import org.int4.dirk.annotations.Produces;

@Singleton
public class DatabaseFactory {
  private static final Logger LOGGER = Logger.getLogger(DatabaseFactory.class.getName());

  @Inject @Nullable @Named("database.url") private String url;
  @Inject @Nullable @Named("database.user") private String user;
  @Inject @Nullable @Named("database.password") private String password;
  @Inject @Nullable @Named("database.postConnectSql") private String postConnectSql;

  private DataSource dataSource;

  @PostConstruct
  private void postConstruct() {
    this.dataSource = url == null ? createDerbyDataSource() : createUserDataSource();
  }

  @Produces
  @Singleton
  Database createDatabase(DatabaseStatementTranslator translator) {
    Supplier<Connection> connectionProvider = () -> createConnection();

    new DatabaseUpdater(connectionProvider, translator).updateDatabase("db-scripts");

    return DatabaseBuilder.using(connectionProvider).build();
  }

  @Produces
  @Singleton
  CheckedDatabase createCheckedDatabase(DatabaseStatementTranslator translator) {
    Supplier<Connection> connectionProvider = () -> createConnection();

    // TODO this is duplicated, although should be safe enough
    new DatabaseUpdater(connectionProvider, translator).updateDatabase("db-scripts");

    return DatabaseBuilder.using(connectionProvider).throwingSQLExceptions();
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

  private static DataSource createDerbyDataSource() {
    HikariConfig config = new HikariConfig();

    config.setJdbcUrl("jdbc:derby:db;create=true");

    return new HikariDataSource(config);
  }

  private Connection createConnection() {
    try {
      return dataSource.getConnection();
    }
    catch(SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  private DataSource createUserDataSource() {
    HikariConfig config = new HikariConfig();

    config.setJdbcUrl(url);
    config.setUsername(user);
    config.setPassword(password);
    config.setConnectionInitSql(postConnectSql);

    return new HikariDataSource(config);
  }
}

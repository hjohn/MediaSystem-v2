package hs.mediasystem.db;

import hs.database.core.ConnectionPool;
import hs.database.core.Database;
import hs.database.core.SimpleConnectionPoolDataSource;
import hs.database.core.SimpleDatabaseStatementTranslator;
import hs.database.schema.DatabaseStatementTranslator;
import hs.database.schema.DatabaseUpdater;
import hs.ddif.annotations.Produces;
import hs.mediasystem.db.core.IdentificationEvent;
import hs.mediasystem.db.core.StreamableEvent;
import hs.mediasystem.db.events.EventSerializer;
import hs.mediasystem.db.events.PersistentEventStore;
import hs.mediasystem.db.events.Serializer;
import hs.mediasystem.db.events.SerializerException;
import hs.mediasystem.db.jackson.RecordSerializer;
import hs.mediasystem.db.jackson.SealedTypeSerializer;
import hs.mediasystem.util.events.cache.CachingEventStore;
import hs.mediasystem.util.events.store.EventStore;

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

  @Produces @Singleton
  EventStore<StreamableEvent> createStreamableEventStore(Database database) {
    Serializer<StreamableEvent> serializer = new SealedTypeSerializer<>(StreamableEvent.class);

    EventSerializer<StreamableEvent> eventSerializer = new EventSerializer<>() {
      @Override
      public byte[] serialize(StreamableEvent value) throws SerializerException {
        return serializer.serialize(value);
      }

      @Override
      public StreamableEvent unserialize(byte[] serialized) throws SerializerException {
        return serializer.unserialize(serialized);
      }

      @Override
      public Type extractType(StreamableEvent event) {
        return switch(event.getClass().getSimpleName()) {
          case "Updated" -> Type.FULL;
          case "Removed" -> Type.DELETE;
          default -> throw new IllegalStateException("Unknown case: " + event.getClass().getSimpleName());
        };
      }

      @Override
      public String extractAggregateId(StreamableEvent event) {
        return "" + event.location().toString();
      }
    };

    return new CachingEventStore<>(new PersistentEventStore<>(database, StreamableEvent.class, "Streamable", eventSerializer));
  }

  @Produces @Singleton
  EventStore<IdentificationEvent> createIdentificationEventStore(Database database) {
    Serializer<IdentificationEvent> serializer = new RecordSerializer<>(IdentificationEvent.class);

    EventSerializer<IdentificationEvent> eventSerializer = new EventSerializer<>() {
      @Override
      public byte[] serialize(IdentificationEvent value) throws SerializerException {
        return serializer.serialize(value);
      }

      @Override
      public IdentificationEvent unserialize(byte[] serialized) throws SerializerException {
        return serializer.unserialize(serialized);
      }

      @Override
      public Type extractType(IdentificationEvent event) {
        return Type.FULL;
      }

      @Override
      public String extractAggregateId(IdentificationEvent event) {
        return "" + event.location().toString();
      }
    };

    return new CachingEventStore<>(new PersistentEventStore<>(database, IdentificationEvent.class, "Identification", eventSerializer));
  }

  @Produces
  @Singleton
  Database createDatabase(DatabaseStatementTranslator translator) {
    Provider<Connection> connectionProvider = () -> createConnection();

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

  private Connection createConnection() {
    try {
      return pool.getConnection();
    }
    catch(InterruptedException e) {
      Thread.currentThread().interrupt();

      throw new RuntimeException("Interrupted while getting connection from pool", e);
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

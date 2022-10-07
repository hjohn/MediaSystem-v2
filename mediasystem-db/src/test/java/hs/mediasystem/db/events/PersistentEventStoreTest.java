package hs.mediasystem.db.events;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.ddif.annotations.Produces;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.util.events.AbstractEventStoreTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

@ExtendWith(InjectorExtension.class)
public class PersistentEventStoreTest extends AbstractEventStoreTest {
  private static final EmbeddedPostgres EMBEDDED_POSTGRES;

  static {
    try {
      EMBEDDED_POSTGRES = EmbeddedPostgres.builder().start();
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @AfterAll
  static void closePostgres() throws IOException {
    EMBEDDED_POSTGRES.close();
  }

  private static final Serializer<String> SERIALIZER = new Serializer<>() {
    @Override
    public byte[] serialize(String value) {
      return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String unserialize(byte[] serialized) {
      return new String(serialized, StandardCharsets.UTF_8);
    }
  };

  @Produces
  static Connection connectionProvider() throws SQLException {
    return EMBEDDED_POSTGRES.getPostgresDatabase().getConnection();
  }

  @Produces
  @Singleton
  static Database createDatabase(Provider<Connection> connectionProvider) {
    return new Database(connectionProvider);
  }

  private final Database database;

  @Inject
  protected PersistentEventStoreTest(Database database) {
    super(new PersistentEventStore<>(database, "testEvents", SERIALIZER), 1);

    this.database = database;
  }

  @AfterEach
  void afterEach() {
    try(Transaction tx = database.beginTransaction()) {
      tx.execute("DROP TABLE IF EXISTS \"events_testEvents\"");
      tx.commit();
    }
  }
}

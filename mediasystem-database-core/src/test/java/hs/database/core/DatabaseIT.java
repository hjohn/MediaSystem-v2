package hs.database.core;

import hs.database.core.Database.Transaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.SingleInstancePostgresExtension;

public class DatabaseIT {

  @RegisterExtension
  public static final SingleInstancePostgresExtension PG = EmbeddedPostgresExtension.singleInstance();

  @Test
  public void test() throws IOException {
    try(EmbeddedPostgres embeddedPostgres = PG.getEmbeddedPostgres()) {
      Database database = new Database(() -> {
        try {
          return embeddedPostgres.getPostgresDatabase().getConnection();
        }
        catch(SQLException e) {
          throw new IllegalStateException(e);
        }
      });

      try(Transaction tx = database.beginTransaction()) {
        tx.execute("CREATE TABLE employee (id serial4, name varchar(100), data bytea)");
        tx.execute("INSERT INTO employee (name, data) VALUES ('John Doe', '\\x001122'::bytea)");
        tx.execute("INSERT INTO employee (name, data) VALUES ('Jane Doe', '\\x010203'::bytea)");

        NameOnly nameOnly = tx.mapOne(Mapper.of(NameOnly.class), "SELECT name FROM employee WHERE id = 1").get();

        assertEquals("John Doe", nameOnly.name());

        All r = tx.mapOne(Mapper.of(All.class), "SELECT id, name, data FROM employee WHERE id = 1").get();

        assertEquals(1, r.id());
        assertEquals("John Doe", r.name());
        assertArrayEquals(new byte[] {0, 0x11, 0x22}, r.data());

        List<NameOnly> names = tx.mapAll(Mapper.of(NameOnly.class), "SELECT name FROM employee");

        assertEquals(2, names.size());
        assertEquals("John Doe", names.get(0).name());
        assertEquals("Jane Doe", names.get(1).name());
      }
    }
  }

  public record NameOnly(String name) {}
  public record All(int id, String name, byte[] data) {}
}

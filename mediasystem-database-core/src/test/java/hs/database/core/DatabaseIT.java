package hs.database.core;

import hs.database.core.Database.Transaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
        tx.execute("CREATE TABLE employee (id serial4, name varchar(100), age int4, data bytea)");
        tx.execute("INSERT INTO employee (name, age, data) VALUES ('John Doe', 32, '\\x001122'::bytea)");
        tx.execute("INSERT INTO employee (name, age, data) VALUES ('Jane Doe', 43, '\\x010203'::bytea)");

        NameOnly nameOnly = tx.mapOne(Mapper.of(NameOnly.class), "SELECT name FROM employee WHERE id = 1").get();

        assertEquals("John Doe", nameOnly.name());

        All r = tx.mapOne(Mapper.of(All.class), "SELECT id, name, age, data FROM employee WHERE id = 1").get();

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

  @Test
  public void shouldExecuteTemplatedQueries() throws IOException {
    try(EmbeddedPostgres embeddedPostgres = PG.getEmbeddedPostgres()) {
      Database database = new Database(() -> {
        try {
          return embeddedPostgres.getPostgresDatabase().getConnection();
        }
        catch(SQLException e) {
          throw new IllegalStateException(e);
        }
      });

      Reflector ALL = Reflector.of(All.class, "id", "name", "age", "data");
      Reflector EXCEPT_ID = ALL.excluding("id");

      try(Transaction tx = database.beginTransaction()) {
        tx.execute("CREATE TABLE employee (id serial4, name varchar(100), age int4, data bytea)");
        tx.executeInsert(Database.SQL."INSERT INTO employee (\{EXCEPT_ID}) VALUES (\{"John Doe"}, 32, \{new byte[] {0, 0x11, 0x22}})");
        tx.executeInsert(Database.SQL."INSERT INTO employee (\{EXCEPT_ID}) VALUES (\{EXCEPT_ID.values(new All(5, "Jane Doe", 43, new byte[] {1, 2, 3}))})");

        assertEquals("John Doe", tx.query(Database.SQL."SELECT name FROM employee WHERE id = 1").asText());

        List<All> list = tx.query(Database.SQL."SELECT \{ALL} FROM employee ORDER BY name").asList(ALL.asMapperFor(All.class));

        assertThat(list.get(0).name).isEqualTo("Jane Doe");
        assertThat(list.get(1).name).isEqualTo("John Doe");

        assertThat(list.get(0).age).isEqualTo(43);
        assertThat(list.get(1).age).isEqualTo(32);

        assertThat(list.get(0).data).isEqualTo(new byte[] {1, 2, 3});
        assertThat(list.get(1).data).isEqualTo(new byte[] {0, 0x11, 0x22});

        assertThat(tx.executeUpdate(Database.SQL."UPDATE employee SET \{EXCEPT_ID.entries(new All(5, "Alice Brooks", 52, new byte[] {2, 3, 5}))} WHERE name = \{"John Doe"}")).isEqualTo(1);

        list = tx.query(Database.SQL."SELECT \{ALL} FROM employee ORDER BY name DESC").asList(ALL.asMapperFor(All.class));

        assertThat(list.get(0).name).isEqualTo("Jane Doe");
        assertThat(list.get(1).name).isEqualTo("Alice Brooks");

        assertThat(list.get(0).age).isEqualTo(43);
        assertThat(list.get(1).age).isEqualTo(52);

        assertThat(list.get(0).data).isEqualTo(new byte[] {1, 2, 3});
        assertThat(list.get(1).data).isEqualTo(new byte[] {2, 3, 5});

        List<Composite> composites = tx.query(Database.SQL."SELECT \{ALL} FROM employee ORDER BY name DESC").asList(ALL.asMapperFor(Composite.class));

        assertThat(composites.get(0).name).isEqualTo("Jane Doe");
        assertThat(composites.get(1).name).isEqualTo("Alice Brooks");

        assertThat(composites.get(0).otherData.age).isEqualTo(43);
        assertThat(composites.get(1).otherData.age).isEqualTo(52);

        assertThat(composites.get(0).otherData.data).isEqualTo(new byte[] {1, 2, 3});
        assertThat(composites.get(1).otherData.data).isEqualTo(new byte[] {2, 3, 5});
      }
    }
  }

  public record NameOnly(String name) {}
  public record All(int id, String name, int age, byte[] data) {}

  public record Composite(int id, String name, OtherData otherData) {}
  public record OtherData(int age, byte[] data) {}
}

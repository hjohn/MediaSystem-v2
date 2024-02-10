package hs.database.core;

import hs.database.core.Database.Transaction;
import hs.database.core.TestEmployee.Hours;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DatabaseTest {
  private static final Date DATE = new Date();

  private Provider<Connection> connectionProvider;
  private Database database;

  @Mock private Connection connection;
  @Mock private Savepoint savepoint;
  @Mock private PreparedStatement statement;
  @Mock private ResultSet employeeResultSet;
  @Mock private ResultSet generatedKeysResultSet;
  @Mock private ResultSetMetaData employeeResultSetMetaData;
  @Mock private ResultSetMetaData generatedKeysResultSetMetaData;

  @BeforeEach
  public void before() throws SQLException {
    when(connection.setSavepoint()).thenReturn(savepoint);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(connection.prepareStatement(anyString(), anyInt())).thenReturn(statement);

    when(statement.executeQuery()).thenReturn(employeeResultSet);
    when(statement.executeUpdate()).thenReturn(1);

    when(employeeResultSet.getMetaData()).thenReturn(employeeResultSetMetaData);
    when(employeeResultSet.next()).thenReturn(true).thenReturn(false);
    when(employeeResultSet.getObject(1)).thenReturn(1001);
    when(employeeResultSet.getObject(2)).thenReturn(false);
    when(employeeResultSet.getObject(3)).thenReturn("Database Joe");
    when(employeeResultSet.getObject(4)).thenReturn("PART_TIME");

    when(employeeResultSetMetaData.getColumnCount()).thenReturn(4);
    when(employeeResultSetMetaData.getColumnName(1)).thenReturn("id");
    when(employeeResultSetMetaData.getColumnName(2)).thenReturn("fired");
    when(employeeResultSetMetaData.getColumnName(3)).thenReturn("name");
    when(employeeResultSetMetaData.getColumnName(4)).thenReturn("hours");
    when(employeeResultSetMetaData.getTableName(1)).thenReturn("employees");
    when(employeeResultSetMetaData.getTableName(2)).thenReturn("employees");
    when(employeeResultSetMetaData.getTableName(3)).thenReturn("employees");
    when(employeeResultSetMetaData.getTableName(4)).thenReturn("employees");

    when(statement.getGeneratedKeys()).thenReturn(generatedKeysResultSet);

    when(generatedKeysResultSet.next()).thenReturn(true).thenReturn(false);
    when(generatedKeysResultSet.getMetaData()).thenReturn(generatedKeysResultSetMetaData);
    when(generatedKeysResultSet.getObject(1)).thenReturn(1001);

    when(generatedKeysResultSetMetaData.getColumnCount()).thenReturn(1);
    when(generatedKeysResultSetMetaData.getColumnName(1)).thenReturn("id");

    connectionProvider = new Provider<>() {
      @Override
      public Connection get() {
        return connection;
      }
    };

    database = new Database(connectionProvider);
  }

  @Test
  public void shouldAutoRollbackTransaction() throws SQLException {
    assertThrows(IllegalArgumentException.class, () -> {
      try(Transaction transaction = database.beginTransaction()) {
        throw new IllegalArgumentException();
      }
    });

    verify(connection).rollback();
    verify(connection, never()).commit();
  }

  @Test
  public void shouldCommitTransaction() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      transaction.commit();
    }

    verify(connection).commit();
    verify(connection, never()).rollback();
  }

  @Test
  public void shouldNotAllowCommitAfterRollback() {
    try(Transaction transaction = database.beginTransaction()) {
      transaction.rollback();

      assertThrows(IllegalStateException.class, () -> transaction.commit());
    }
  }

  @Test
  public void shouldNotAllowRollbackAfterCommit() {
    try(Transaction transaction = database.beginTransaction()) {
      transaction.commit();

      assertThrows(IllegalStateException.class, () -> transaction.rollback());
    }
  }

  @Test
  public void shouldAllowNestedTransaction() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      try(Transaction nestedTransaction = database.beginTransaction()) {
        nestedTransaction.commit();
      }

      verify(connection, never()).rollback();
      verify(connection, never()).commit();

      transaction.commit();
    }

    verify(connection).commit();
  }

  @Test
  public void shouldNotAllowUncommitedNestedTransactions() {
    try(Transaction transaction = database.beginTransaction()) {
      try(Transaction nestedTransaction = database.beginTransaction()) {
        assertThrows(DatabaseException.class, () -> transaction.commit());
      }
    }
  }

  @Test
  public void shouldNotAllowWriteOperationsWithinReadOnlyTransactions() {
    try(Transaction transaction = database.beginReadOnlyTransaction()) {
      assertThrows(DatabaseException.class, () -> transaction.insert(new TestEmployee()));
    }
  }

  @Test
  public void shouldInsertRowAndReturnGeneratedKey() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      Object key = transaction.insert("items", new LinkedHashMap<String, Object>() {{
        put("childcount", 2);
        put("creationtime", DATE);
      }});

      assertEquals(1001, key);
    }

    verify(connection).prepareStatement("INSERT INTO items (childcount,creationtime) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
    verify(statement).setObject(1, 2);
    verify(statement).setTimestamp(2, new Timestamp(DATE.getTime()));
  }

  @Test
  public void shouldUpdateRow() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      transaction.update("items", 15, new LinkedHashMap<String, Object>() {{
        put("childcount", 2);
        put("changetime", DATE);
      }});
    }

    verify(connection).prepareStatement("UPDATE items SET childcount=?,changetime=? WHERE id = ?");
    verify(statement).setObject(1, 2);
    verify(statement).setTimestamp(2, new Timestamp(DATE.getTime()));
    verify(statement).setObject(3, 15);
  }

  @Test
  public void shouldSelectRow() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      DatabaseRecord databaseRecord = transaction.selectUnique("*", "employees", "id=?", 1001);

      assertEquals(Integer.valueOf(1001), databaseRecord.getInteger("id"));
      assertEquals("PART_TIME", databaseRecord.getString("hours"));
      assertEquals("Database Joe", databaseRecord.getString("name"));
    }

    verify(connection).prepareStatement("SELECT * FROM employees WHERE id=?");
    verify(statement).setObject(1, 1001);
  }

  @Test
  public void shouldThrowDatabaseExceptionWhenSelectFails() throws SQLException {
    when(statement.executeQuery()).thenThrow(new SQLException());

    try(Transaction transaction = database.beginTransaction()) {
      assertThrows(DatabaseException.class, () -> transaction.selectUnique("*", "employees", "id=?", 1001));
    }
  }

  @Test
  public void shouldInsertObjectAndSetId() throws SQLException {
    TestEmployee testEmployee = new TestEmployee();

    testEmployee.setName("John Doe");
    testEmployee.setHours(Hours.FULL_TIME);

    try(Transaction transaction = database.beginTransaction()) {
      transaction.insert(testEmployee);
    }

    assertEquals(Integer.valueOf(1001), testEmployee.getId());
    verify(connection).prepareStatement("INSERT INTO employees (employers_id,fired,hours,name) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
    verify(statement).setObject(1, null);
    verify(statement).setObject(2, false);
    verify(statement).setObject(3, "FULL_TIME");
    verify(statement).setObject(4, "John Doe");
  }

  @Test
  public void shouldUpdateObject() throws SQLException {
    TestEmployee testEmployee = new TestEmployee();

    testEmployee.setId(2);
    testEmployee.setName("John Doe");
    testEmployee.setHours(Hours.FULL_TIME);

    try(Transaction transaction = database.beginTransaction()) {
      transaction.update(testEmployee);
    }

    verify(connection).prepareStatement("UPDATE employees SET employers_id=?,fired=?,hours=?,name=? WHERE id = ?");
    verify(statement).setObject(1, null);
    verify(statement).setObject(2, false);
    verify(statement).setObject(3, "FULL_TIME");
    verify(statement).setObject(4, "John Doe");
    verify(statement).setObject(5, 2);
  }

  @Test
  public void shouldSelectObjectAndInvokeAfterLoadStore() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      TestEmployee employee = transaction.selectUnique(TestEmployee.class, "id=?", 1001);

      assertEquals(Integer.valueOf(1001), employee.getId());
      assertEquals(Hours.PART_TIME, employee.getHours());
      assertEquals("Database Joe", employee.getName());
      assertEquals(new Date(2), employee.getLastLoad());
    }

    verify(connection).prepareStatement("SELECT * FROM employees WHERE id=?");
    verify(statement).setObject(1, 1001);
  }

  @Test
  public void shouldThrowExceptionWhenSelectObjectClassIsMissingEmptyConstructor() {
    try(Transaction transaction = database.beginTransaction()) {
      assertThrows(DatabaseException.class, () -> transaction.selectUnique(TestBadEmployeeNoEmptyConstructor.class, "id=?", 1001));
    }
  }

  @Test
  public void shouldDeleteRows() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      int deleteCount = transaction.delete("mediadata", "uri = ? OR hash = ?", "TEST_URI", "TEST_HASH");

      assertEquals(1, deleteCount);
    }

    verify(connection).prepareStatement("DELETE FROM mediadata WHERE uri = ? OR hash = ?");
    verify(statement).setObject(1, "TEST_URI");
    verify(statement).setObject(2, "TEST_HASH");
  }

  @Test
  public void shouldDeleteChildren() throws SQLException {
    try(Transaction transaction = database.beginTransaction()) {
      transaction.deleteChildren("castings", "items", 15);
    }

    verify(connection).prepareStatement("DELETE FROM castings WHERE items_id = ?");
    verify(statement).setLong(1, 15);
  }
}

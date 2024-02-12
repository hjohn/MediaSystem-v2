package hs.database.core;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RestrictedResultSet {
  private final ResultSet rs;

  RestrictedResultSet(ResultSet rs) {
    this.rs = rs;
  }

  public String getString(String name) throws SQLException {
    return rs.getString(name);
  }

  public String getString(int columnIndex) throws SQLException {
    return rs.getString(columnIndex);
  }

  public Object getObject(String name, Class<?> type) throws SQLException {
    return rs.getObject(name, type);
  }

  public Object getObject(int columnIndex, Class<?> type) throws SQLException {
    return rs.getObject(columnIndex, type);
  }

  public int getInt(String name) throws SQLException {
    return rs.getInt(name);
  }

  public int getInt(int columnIndex) throws SQLException {
    return rs.getInt(columnIndex);
  }

  public long getLong(String name) throws SQLException {
    return rs.getLong(name);
  }

  public long getLong(int columnIndex) throws SQLException {
    return rs.getLong(columnIndex);
  }

  public double getDouble(String name) throws SQLException {
    return rs.getDouble(name);
  }

  public double getDouble(int columnIndex) throws SQLException {
    return rs.getDouble(columnIndex);
  }

  public byte[] getBytes(String name) throws SQLException {
    return rs.getBytes(name);
  }

  public byte[] getBytes(int columnIndex) throws SQLException {
    return rs.getBytes(columnIndex);
  }
}

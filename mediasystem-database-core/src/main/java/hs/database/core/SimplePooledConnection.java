package hs.database.core;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

public class SimplePooledConnection implements PooledConnection {
  private final List<ConnectionEventListener> connectionEventListeners = new ArrayList<>();
  private final Connection connection;
  private final ConnectionWrapper connectionWrapper;

  public SimplePooledConnection(Connection connection) {
    this.connection = connection;
    this.connectionWrapper = new ConnectionWrapper(connection);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return connectionWrapper;
  }

  @Override
  public void close() throws SQLException {
    connection.close();
  }

  @Override
  public void addConnectionEventListener(ConnectionEventListener listener) {
    connectionEventListeners.add(listener);
  }

  @Override
  public void removeConnectionEventListener(ConnectionEventListener listener) {
    connectionEventListeners.remove(listener);
  }

  @Override
  public void addStatementEventListener(StatementEventListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeStatementEventListener(StatementEventListener listener) {
    throw new UnsupportedOperationException();
  }

  private class ConnectionWrapper implements Connection {
    private final Connection wrappedConnection;

    public ConnectionWrapper(Connection connection) {
      this.wrappedConnection = connection;
    }

    private void notifyError(SQLException e) {
      ConnectionEvent event = new ConnectionEvent(SimplePooledConnection.this, e);

      for(ConnectionEventListener listener : new ArrayList<>(connectionEventListeners)) {
        listener.connectionErrorOccurred(event);
      }
    }

    @Override
    public void close() throws SQLException {
      ConnectionEvent event = new ConnectionEvent(SimplePooledConnection.this);

      for(ConnectionEventListener listener : new ArrayList<>(connectionEventListeners)) {
        listener.connectionClosed(event);
      }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      try {
        return wrappedConnection.unwrap(iface);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      try {
        return wrappedConnection.isWrapperFor(iface);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Statement createStatement() throws SQLException {
      try {
        return wrappedConnection.createStatement();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
      try {
        return wrappedConnection.prepareStatement(sql);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
      try {
        return wrappedConnection.prepareCall(sql);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
      try {
        return wrappedConnection.nativeSQL(sql);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      try {
        wrappedConnection.setAutoCommit(autoCommit);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
      try {
        return wrappedConnection.getAutoCommit();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void commit() throws SQLException {
      try {
        wrappedConnection.commit();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void rollback() throws SQLException {
      try {
        wrappedConnection.rollback();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public boolean isClosed() throws SQLException {
      try {
        return wrappedConnection.isClosed();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
      try {
        return wrappedConnection.getMetaData();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
      try {
        wrappedConnection.setReadOnly(readOnly);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
      try {
        return wrappedConnection.isReadOnly();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
      try {
        wrappedConnection.setCatalog(catalog);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public String getCatalog() throws SQLException {
      try {
        return wrappedConnection.getCatalog();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
      try {
        wrappedConnection.setTransactionIsolation(level);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
      try {
        return wrappedConnection.getTransactionIsolation();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
      try {
        return wrappedConnection.getWarnings();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void clearWarnings() throws SQLException {
      try {
        wrappedConnection.clearWarnings();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
      try {
        return wrappedConnection.createStatement(resultSetType, resultSetConcurrency);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
      try {
        return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
      try {
        return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
      try {
        return wrappedConnection.getTypeMap();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
      try {
        wrappedConnection.setTypeMap(map);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
      try {
        wrappedConnection.setHoldability(holdability);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public int getHoldability() throws SQLException {
      try {
        return wrappedConnection.getHoldability();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
      try {
        return wrappedConnection.setSavepoint();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
      try {
        return wrappedConnection.setSavepoint(name);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
      try {
        wrappedConnection.rollback(savepoint);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
      try {
        wrappedConnection.releaseSavepoint(savepoint);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      try {
        return wrappedConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      try {
        return wrappedConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      try {
        return wrappedConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
      try {
        return wrappedConnection.prepareStatement(sql, autoGeneratedKeys);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
      try {
        return wrappedConnection.prepareStatement(sql, columnIndexes);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
      try {
        return wrappedConnection.prepareStatement(sql, columnNames);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Clob createClob() throws SQLException {
      try {
        return wrappedConnection.createClob();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Blob createBlob() throws SQLException {
      try {
        return wrappedConnection.createBlob();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public NClob createNClob() throws SQLException {
      try {
        return wrappedConnection.createNClob();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
      try {
        return wrappedConnection.createSQLXML();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
      try {
        return wrappedConnection.isValid(timeout);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
      wrappedConnection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
      wrappedConnection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
      try {
        return wrappedConnection.getClientInfo(name);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Properties getClientInfo() throws SQLException {
      try {
        return wrappedConnection.getClientInfo();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
      try {
        return wrappedConnection.createArrayOf(typeName, elements);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
      try {
        return wrappedConnection.createStruct(typeName, attributes);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setSchema(String schema) throws SQLException {
      try {
        wrappedConnection.setSchema(schema);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public String getSchema() throws SQLException {
      try {
        return wrappedConnection.getSchema();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void abort(Executor executor) throws SQLException {
      try {
        wrappedConnection.abort(executor);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
      try {
        wrappedConnection.setNetworkTimeout(executor, milliseconds);
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
      try {
        return wrappedConnection.getNetworkTimeout();
      }
      catch(SQLException e) {
        notifyError(e);
        throw e;
      }
    }
  }
}

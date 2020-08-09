package hs.database.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

/**
 * JDBC connection pool for use with ConnectionPoolDataSource objects.
 */
public class ConnectionPool implements AutoCloseable {
  private final Queue<TimedPooledConnection> connections = new ArrayDeque<>();
  private final ConnectionEventListener poolConnectionEventListener = new ConnectionEventListener() {
    @Override
    public void connectionClosed(ConnectionEvent event) {
      returnConnection((PooledConnection)event.getSource(), false);
    }

    @Override
    public void connectionErrorOccurred(ConnectionEvent event) {
      returnConnection((PooledConnection)event.getSource(), true);
    }
  };

  private final ConnectionPoolDataSource dataSource;
  private final Semaphore semaphore;
  private final int maxConnections;
  private final int timeOutMillis;

  private boolean useFallBackValidTest;
  private boolean isClosed;

  private static class TimedPooledConnection {
    private final PooledConnection pooledConnection;
    private final long creationTime;

    public TimedPooledConnection(PooledConnection pooledConnection) {
      this.pooledConnection = pooledConnection;
      this.creationTime = System.currentTimeMillis();
    }

    public boolean hasExpired() {
      return creationTime + 300 * 1000L < System.currentTimeMillis();
    }

    public void close() throws SQLException {
      pooledConnection.close();
    }

    @Override
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if(o == null || getClass() != o.getClass()) {
        return false;
      }

      return pooledConnection.equals(((TimedPooledConnection)o).pooledConnection);
    }

    @Override
    public int hashCode() {
      return pooledConnection.hashCode();
    }
  }

  public synchronized void closeIdleConnections() {
    Iterator<TimedPooledConnection> iterator = connections.iterator();

    while(iterator.hasNext()) {
      TimedPooledConnection connection = iterator.next();

      if(connection.hasExpired()) {
        iterator.remove();

        try {
          connection.close();
        }
        catch(SQLException e) {
          // Not interested
        }
      }
    }
  }

  /**
   * Constructs a ConnectionPool object.
   *
   * @param dataSource data source to obtain pooled connections from
   * @param maxConnections maximum number of connections
   * @param timeOutMillis maximum time to wait for an available connection
   */
  public ConnectionPool(ConnectionPoolDataSource dataSource, int maxConnections, int timeOutMillis) {
    if(maxConnections < 1) {
      throw new IllegalArgumentException("Parameter 'maxConnections' must be > 0");
    }

    this.dataSource = dataSource;
    this.maxConnections = maxConnections;
    this.timeOutMillis = timeOutMillis;

    semaphore = new Semaphore(maxConnections, true);
  }

  /**
   * Constructs a ConnectionPool object with a time-out of 60 seconds.
   *
   * @param dataSource data source to obtain pooled connections from
   * @param maxConnections maximum number of connections
   */
  public ConnectionPool(ConnectionPoolDataSource dataSource, int maxConnections) {
    this(dataSource, maxConnections, 10 * 1000);
  }

  /**
   * Returns a connection from the connection pool. If the maximum number of
   * connections would be exceeded, then this method waits until a connection
   * becomes available or until the time out expires.<p>
   *
   * Connections can be returned to the pool by closing them.
   *
   * @throws TimeOutException when the time out expired while attempting to get a connection
   * @throws InterruptedException when interrupted while waiting for a connection to become available
   * @return a new Connection object.
   */
  public Connection getConnection() throws InterruptedException {
    synchronized(this) {
      closeIdleConnections();

      if(isClosed) {
        throw new IllegalStateException("Connection pool is closed");
      }
    }

    if(!semaphore.tryAcquire(timeOutMillis, TimeUnit.MILLISECONDS)) {
      throw new TimeOutException("Unable to acquire connection in " + timeOutMillis + " ms");
    }

    try {
      return getValidConnection();
    }
    catch(Exception t) {
      semaphore.release();
      throw new IllegalStateException("Unable to acquire connection", t);
    }
  }

  /**
   * Closes all connections in this connection pool.
   */
  @Override
  public synchronized void close() {
    if(isClosed) {
      return;
    }

    SQLException firstException = null;

    for(TimedPooledConnection connection : connections) {
      try {
        connection.close();
      }
      catch(SQLException e) {
        if(firstException == null) {
          firstException = e;
        }
      }
    }

    isClosed = true;

    if(firstException != null) {
      throw new RuntimeException("Exception while closing connection(s)", firstException);
    }
  }

  private boolean isValidConnection(Connection connection) {
    if(!useFallBackValidTest) {
      try {
        return connection.isValid(1);
      }
      catch(Exception e) {
        useFallBackValidTest = true;
      }
    }

    try {
      try(PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
        statement.execute();
      }
      return true;
    }
    catch(SQLException e) {
      return false;
    }
  }

  private synchronized Connection getValidConnection() throws SQLException {
    if(isClosed) {
      throw new IllegalStateException("Connection pool is closed");
    }

    for(int tries = 0; tries < 3; tries++) {
      PooledConnection pooledConnection = connections.isEmpty() ? dataSource.getPooledConnection() : connections.remove().pooledConnection;
      Connection connection = pooledConnection.getConnection();

      if(isValidConnection(connection)) {
        pooledConnection.addConnectionEventListener(poolConnectionEventListener);
        return connection;
      }
    }

    throw new SQLException("Could not get a valid connection");
  }

  private synchronized void returnConnection(PooledConnection connection, boolean errorOccured) {
    connection.removeConnectionEventListener(poolConnectionEventListener);

    if(maxConnections == semaphore.availablePermits()) {
      throw new IllegalStateException("Attempt to return more connections than were used: " + connection);
    }

    semaphore.release();

    if(errorOccured || isClosed) {
      try {
        connection.close();
      }
      catch(SQLException e) {
        System.out.println("[FINE] ConnectionPool.closeConnection() - exception while closing connection: " + e);
      }
    }
    else {
      connections.add(new TimedPooledConnection(connection));
    }
  }
}

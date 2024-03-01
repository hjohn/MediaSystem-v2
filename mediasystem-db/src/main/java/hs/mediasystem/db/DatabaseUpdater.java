package hs.mediasystem.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DatabaseUpdater {
  private static final Logger LOGGER = Logger.getLogger(DatabaseUpdater.class.getName());

  private final Supplier<Connection> connectionProvider;
  private final DatabaseStatementTranslator translator;

  public DatabaseUpdater(Supplier<Connection> connectionProvider, DatabaseStatementTranslator translator) {
    this.connectionProvider = connectionProvider;
    this.translator = translator;
  }

  public void updateDatabase(String resourcePath) {
    Version version = getDatabaseVersion();
    Version currentVersion = version;

    try {
      if(version == Version.ZERO) {
        // Find highest major version first if starting from scratch:
        for(;;) {
          String scriptName = resourcePath + "/db-v" + version.nextMajor() + ".sql";

          LOGGER.fine("Checking for highest major database version script at: " + scriptName);

          URL url = getClass().getClassLoader().getResource(scriptName);

          if(url == null) {
            break;
          }

          version = version.nextMajor();
        }

        LOGGER.info("Creating initial database from version: " + version);
      }
      else {
        version = version.nextMinor();
      }

      for(;;) {
        String scriptName = resourcePath + "/db-v" + version + ".sql";

        LOGGER.fine("Checking for newer database version update script at: " + scriptName);

        URL url = getClass().getClassLoader().getResource(scriptName);

        if(url == null) {
          version = version.nextMajor();

          scriptName = resourcePath + "/db-v" + version + ".sql";

          LOGGER.fine("Checking for newer major database version: " + version);

          if(getClass().getClassLoader().getResource(scriptName) != null) {
            setDatabaseVersion(version);

            currentVersion = version;
            version = version.nextMinor();
            continue;  // major version found, although the first new major version script must be skipped, more minor versions may follow
          }

          break;  // no major version found after checking minor versions, database is up to date now
        }

        try(InputStream sqlStream = url.openStream()) {
          LOGGER.info("Updating database to version " + version);

          try {
            applyUpdateScript(version, sqlStream);

            currentVersion = version;
            version = version.nextMinor();
          }
          catch (Exception e) {
            throw new DatabaseUpdateException("Exception while executing update script: " + scriptName, e);
          }
        }
      }

      LOGGER.info("Database up to date at version " + currentVersion);
    }
    catch(IOException | SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Sets the database version.  Useful for debugging scripts.
   *
   * @param version a {@link Version} to set, cannot be {@code null}
   */
  public void setDatabaseVersion(Version version) {
    try {
      try(Connection connection = connectionProvider.get()) {
        try {
          connection.setAutoCommit(false);

          updateDatabaseVersion(version, connection);

          LOGGER.info("Forcing database to version " + version);

          connection.commit();
        }
        finally {
          if(!connection.isClosed()) {
            connection.rollback();
          }
        }
      }
    }
    catch(SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static void updateDatabaseVersion(Version version, Connection connection) throws SQLException {
    try(PreparedStatement statement = connection.prepareStatement("UPDATE dbinfo SET value = '" + version + "' WHERE name = 'version'")) {
      if(statement.executeUpdate() != 1) {
        throw new IllegalStateException("Unable to update version information to " + version);
      }
    }
  }

  private void applyUpdateScript(Version version, InputStream sqlStream) throws SQLException, IOException {
    try(Connection connection = connectionProvider.get()) {
      try {
        connection.setAutoCommit(false);

        try(LineNumberReader reader = new LineNumberReader(new InputStreamReader(sqlStream))) {

          statementExecuteLoop:
          for(;;) {
            String sqlStatement = "";

            while(!sqlStatement.endsWith(";")) {
              String line = reader.readLine();

              if(line == null) {
                if(!sqlStatement.trim().isEmpty()) {
                  throw new DatabaseUpdateException("Unexpected EOF, last statement was: " + sqlStatement);
                }

                break statementExecuteLoop;
              }

              int hash = line.indexOf('#');

              if(hash >= 0) {
                line = line.substring(0, hash);
              }

              sqlStatement += line;
            }

            sqlStatement = translator.translate(sqlStatement.substring(0, sqlStatement.length() - 1));  // strip off semi-colon

            try(PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
              LOGGER.fine(sqlStatement);

              statement.execute();
            }
            catch(SQLException e) {
              throw new DatabaseUpdateException("Exception at line " + reader.getLineNumber() + ": " + sqlStatement, e);
            }
          }
        }

        updateDatabaseVersion(version, connection);

        connection.commit();
      }
      finally {
        if(!connection.isClosed()) {
          connection.rollback();
        }
      }
    }
  }

  private Version getDatabaseVersion() {
    try(Connection connection = connectionProvider.get()) {
      DatabaseMetaData dbm = connection.getMetaData();

      try(ResultSet rs1 = dbm.getTables(null, null, "dbinfo", null);
          ResultSet rs2 = dbm.getTables(null, null, "DBINFO", null)) {
        if(!rs1.next() && !rs2.next()) {
          LOGGER.fine("No dbinfo table exists, returning version 0");

          return Version.ZERO;
        }
      }

      try(PreparedStatement statement = connection.prepareStatement("SELECT value FROM dbinfo WHERE name = 'version'")) {
        try(ResultSet rs = statement.executeQuery()) {
          if(rs.next()) {
            return new Version(rs.getString("value"));
          }
        }
      }
    }
    catch(SQLException e) {
      throw new IllegalStateException("Unable to get version information from the database", e);
    }

    return Version.ZERO;
  }

  public static class Version {
    static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+\\.)?[0-9]+");
    static final Version ZERO = new Version("0.0");

    final int major;
    final int minor;

    Version(String version) {
      if(!VERSION_PATTERN.matcher(version).matches()) {
        throw new IllegalArgumentException("Unsupported version format, should be <major>.<minor>: " + version);
      }

      String[] parts = version.split("\\.");

      if(parts.length == 1) {
        this.major = 0;
        this.minor = Integer.parseInt(parts[0]);
      }
      else {
        this.major = Integer.parseInt(parts[0]);
        this.minor = Integer.parseInt(parts[1]);
      }
    }

    public Version(int major, int minor) {
      this.major = major;
      this.minor = minor;
    }

    Version nextMinor() {
      return new Version(major, minor + 1);
    }

    Version nextMajor() {
      return new Version(major + 1, 0);
    }

    @Override
    public String toString() {
      return major + "." + minor;
    }
  }
}

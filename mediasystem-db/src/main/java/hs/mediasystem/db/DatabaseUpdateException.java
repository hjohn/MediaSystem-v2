package hs.mediasystem.db;

import java.sql.SQLException;

public class DatabaseUpdateException extends SQLException {

  public DatabaseUpdateException(String message, Throwable cause) {
    super(message, cause);
  }

  public DatabaseUpdateException(String message) {
    super(message);
  }
}

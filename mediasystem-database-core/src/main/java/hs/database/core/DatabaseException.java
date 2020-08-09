package hs.database.core;

import hs.database.core.Database.Transaction;

public class DatabaseException extends RuntimeException {

  public DatabaseException(Transaction transaction, String message, Throwable cause) {
    super(transaction + ": " + message, cause);
  }

  public DatabaseException(Transaction transaction, String message) {
    super(transaction + ": " + message);
  }
}

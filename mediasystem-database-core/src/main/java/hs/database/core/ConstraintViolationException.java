package hs.database.core;

import hs.database.core.Database.Transaction;

public class ConstraintViolationException extends DatabaseException {

  public ConstraintViolationException(Transaction transaction, String message, Throwable cause) {
    super(transaction, message, cause);
  }

  public ConstraintViolationException(Transaction transaction, String message) {
    super(transaction, message);
  }
}

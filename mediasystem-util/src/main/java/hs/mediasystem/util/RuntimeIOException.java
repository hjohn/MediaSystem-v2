package hs.mediasystem.util;

public class RuntimeIOException extends RuntimeException {

  public RuntimeIOException() {
    super();
  }

  public RuntimeIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public RuntimeIOException(String message, Throwable cause) {
    super(message, cause);
  }

  public RuntimeIOException(String message) {
    super(message);
  }

  public RuntimeIOException(Throwable cause) {
    super(cause);
  }
}

package hs.mediasystem.util;

public class WrappedCheckedException extends RuntimeException {

  public WrappedCheckedException(Throwable throwable) {
    super(throwable);

    if(throwable instanceof RuntimeException) {
      throw new IllegalArgumentException("throwable must not be a RuntimeException: " + throwable);
    }
  }

}

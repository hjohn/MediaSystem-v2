package hs.mediasystem.util.exception;

public class WrappedCheckedException extends RuntimeException {

  public WrappedCheckedException(Throwable throwable) {
    super(throwable);

    if(throwable instanceof RuntimeException) {
      throw new IllegalArgumentException("throwable must not be a RuntimeException: " + throwable);
    }
  }

}

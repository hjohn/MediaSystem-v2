package hs.mediasystem.util.exception;

import java.util.Objects;

public class ExceptionalException extends RuntimeException {

  public ExceptionalException(Throwable cause) {
    super(cause);

    if(cause == null) {
      throw new IllegalArgumentException("cause cannot be null");
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCause());
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ExceptionalException other = (ExceptionalException)obj;

    if(!getCause().equals(other.getCause())) {
      return false;
    }

    return true;
  }
}

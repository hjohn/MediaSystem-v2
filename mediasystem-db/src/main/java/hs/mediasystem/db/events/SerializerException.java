package hs.mediasystem.db.events;

import java.util.Objects;

/**
 * Thrown to indicate a problem during serialization or deserialization.
 */
public class SerializerException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   * @param cause a cause, can be {@code null}
   * @throws NullPointerException when {@code message} is {@code null}
   */
  public SerializerException(String message, Throwable cause) {
    super(Objects.requireNonNull(message, "message"), cause);
  }
}

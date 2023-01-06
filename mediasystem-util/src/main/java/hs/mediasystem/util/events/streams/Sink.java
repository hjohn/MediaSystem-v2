package hs.mediasystem.util.events.streams;

import java.util.List;

public interface Sink<T> {

  /**
   * Pushes the given object to the sink.
   *
   * @param object an object, cannot be {@code null}
   * @throws NullPointerException when object is {@code null}
   */
  void push(T object);

  /**
   * Pushes the given objects to the sink. If any of the
   * elements in the list are {@code null}, these are silently skipped.
   *
   * @param objects a list of objects, cannot be {@code null}
   * @throws NullPointerException when objects is {@code null}
   */
  void push(List<T> objects);

}

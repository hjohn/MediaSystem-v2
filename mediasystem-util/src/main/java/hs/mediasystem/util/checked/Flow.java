package hs.mediasystem.util.checked;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Wraps a standard {@link Stream} and allows handling of a checked exception.  Intermediate
 * operations all accept functions which throw a checked exception, while terminal
 * operations will declare the exception to be thrown.
 *
 * @param <T> the type of the {@link Stream}
 * @param <E> the type of exception to allow
 */
public class Flow<T, E extends Exception> {
  private final Stream<T> stream;
  private final Class<E> exceptionType;

  public static <T, E extends Exception> Flow<T, E> of(Stream<T> stream, Class<E> exceptionType) {
    return new Flow<>(stream, exceptionType);
  }

  public static <T, E extends Exception> Flow<T, E> of(Collection<T> collection, Class<E> exceptionType) {
    return new Flow<>(collection.stream(), exceptionType);
  }

  public static <T> Flow<T, IOException> forIOException(Stream<T> stream) {
    return new Flow<>(stream, IOException.class);
  }

  public static <T> Flow<T, IOException> forIOException(Collection<T> collection) {
    return new Flow<>(collection.stream(), IOException.class);
  }

  private Flow(Stream<T> stream, Class<E> exceptionType) {
    this.stream = stream;
    this.exceptionType = exceptionType;
  }

  /*
   * Intermediate operations:
   */

  public Flow<T, E> filter(ThrowingPredicate<? super T, E> predicate) {
    return new Flow<>(stream.filter(x -> handlePredicate(predicate, x)), exceptionType);
  }

  public <R> Flow<R, E> flatMap(ThrowingFunction<? super T, ? extends Stream<? extends R>, E> mapper) {
    return new Flow<>(stream.flatMap(x -> handleFunction(mapper, x)), exceptionType);
  }

  public <R> Flow<R, E> map(ThrowingFunction<? super T, ? extends R, E> mapper) {
    return new Flow<>(stream.map(x -> handleFunction(mapper, x)), exceptionType);
  }

  public Flow<T, E> peek(ThrowingConsumer<? super T, E> action) {
    return new Flow<>(stream.peek(x -> handleConsumer(action, x)), exceptionType);
  }

  private <U> void handleConsumer(ThrowingConsumer<U, E> func, U x) {
    try {
      func.accept(x);
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      if(exceptionType.isInstance(e)) {
        throw new WrapperException(e);
      }

      throw new IllegalStateException(e);  // Shouldn't actually occur as that means a different checked exception type was snuck in
    }
  }

  private <R, U> R handleFunction(ThrowingFunction<U, ? extends R, E> func, U x) {
    try {
      return func.apply(x);
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      if(exceptionType.isInstance(e)) {
        throw new WrapperException(e);
      }

      throw new IllegalStateException(e);  // Shouldn't actually occur as that means a different checked exception type was snuck in
    }
  }

  private <U> boolean handlePredicate(ThrowingPredicate<U, E> func, U x) {
    try {
      return func.test(x);
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      if(exceptionType.isInstance(e)) {
        throw new WrapperException(e);
      }

      throw new IllegalStateException(e);  // Shouldn't actually occur as that means a different checked exception type was snuck in
    }
  }

  /*
   * Terminal operations:
   */

  @SuppressWarnings("unchecked")
  public <R, A> R collect(Collector<? super T, A, R> collector) throws E {
    try {
      return stream.collect(collector);
    }
    catch(WrapperException e) {
      throw (E)e.getCause();
    }
  }

  /*
   * Support stuff:
   */

  private static class WrapperException extends RuntimeException {
    WrapperException(Exception cause) {
      super(cause);
    }
  }
}
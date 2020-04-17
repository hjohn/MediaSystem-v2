package hs.mediasystem.util.checked;

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
public class CheckedStream<T, E extends Exception> {
  private final Stream<T> stream;
  private final Class<E> exceptionType;

  public static <T, E extends Exception> CheckedStream<T, E> of(Stream<T> stream, Class<E> exceptionType) {
    return new CheckedStream<>(stream, exceptionType);
  }

  private CheckedStream(Stream<T> stream, Class<E> exceptionType) {
    this.stream = stream;
    this.exceptionType = exceptionType;
  }

  public CheckedStream<T, E> filter(ThrowingPredicate<? super T, E> predicate) {
    return new CheckedStream<>(stream.filter(x -> handlePredicate(predicate, x)), exceptionType);
  }

  public <R> CheckedStream<R, E> flatMap(ThrowingFunction<? super T, ? extends Stream<? extends R>, E> mapper) {
    return new CheckedStream<>(stream.flatMap(x -> handleFunction(mapper, x)), exceptionType);
  }

  public <R> CheckedStream<R, E> map(ThrowingFunction<? super T, ? extends R, E> mapper) {
    return new CheckedStream<>(stream.map(x -> handleFunction(mapper, x)), exceptionType);
  }

  public CheckedStream<T, E> peek(ThrowingConsumer<? super T, E> action) {
    return new CheckedStream<>(stream.peek(x -> handleConsumer(action, x)), exceptionType);
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

  @SuppressWarnings("unchecked")
  public <R, A> R collect(Collector<? super T, A, R> collector) throws E {
    try {
      return stream.collect(collector);
    }
    catch(WrapperException e) {
      throw (E)e.getCause();
    }
  }

  private static class WrapperException extends RuntimeException {
    WrapperException(Exception cause) {
      super(cause);
    }
  }
}
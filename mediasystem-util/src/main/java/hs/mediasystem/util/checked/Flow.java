package hs.mediasystem.util.checked;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Wraps a standard {@link Stream} and allows handling of a checked exception.  Intermediate
 * operations all accept functions which throw a checked exception, while terminal
 * operations will declare the exception to be thrown.
 *
 * @param <T> the type of the {@link Stream}
 * @param <E> the type of exception to allow
 * @param <F> the second type of exception to allow
 */
public class Flow<T, E extends Exception, F extends Exception> {
  private final Stream<T> stream;
  private final Class<E> exceptionType;
  private final Class<F> exceptionType2;

  public static <T, E extends Exception, F extends Exception> Flow<T, E, F> of(Stream<T> stream, Class<E> exceptionType, Class<F> exceptionType2) {
    return new Flow<>(stream, exceptionType, exceptionType2);
  }

  public static <T, E extends Exception> Flow<T, E, E> of(Stream<T> stream, Class<E> exceptionType) {
    return new Flow<>(stream, exceptionType, null);
  }

  public static <T, E extends Exception, F extends Exception> Flow<T, E, F> of(Collection<T> collection, Class<E> exceptionType, Class<F> exceptionType2) {
    return new Flow<>(collection.stream(), exceptionType, exceptionType2);
  }

  public static <T, E extends Exception> Flow<T, E, E> of(Collection<T> collection, Class<E> exceptionType) {
    return new Flow<>(collection.stream(), exceptionType, null);
  }

  public static <T> Flow<T, IOException, IOException> forIOException(Stream<T> stream) {
    return new Flow<>(stream, IOException.class, null);
  }

  public static <T> Flow<T, IOException, IOException> forIOException(Collection<T> collection) {
    return new Flow<>(collection.stream(), IOException.class, null);
  }

  private Flow(Stream<T> stream, Class<E> exceptionType, Class<F> exceptionType2) {
    this.stream = stream;
    this.exceptionType = exceptionType;
    this.exceptionType2 = exceptionType2;
  }

  /*
   * Intermediate operations:
   */

  public Flow<T, E, F> filter(ThrowingPredicate<? super T, E, F> predicate) {
    return new Flow<>(stream.filter(x -> handlePredicate(predicate, x)), exceptionType, exceptionType2);
  }

  public <R> Flow<R, E, F> flatMap(ThrowingFunction<? super T, ? extends Stream<? extends R>, E, F> mapper) {
    return new Flow<>(stream.flatMap(x -> handleFunction(mapper, x)), exceptionType, exceptionType2);
  }

  public <R> Flow<R, E, F> map(ThrowingFunction<? super T, ? extends R, E, F> mapper) {
    return new Flow<>(stream.map(x -> handleFunction(mapper, x)), exceptionType, exceptionType2);
  }

  public Flow<T, E, F> peek(ThrowingConsumer<? super T, E, F> action) {
    return new Flow<>(stream.peek(x -> handleConsumer(action, x)), exceptionType, exceptionType2);
  }

  private <U> void handleConsumer(ThrowingConsumer<U, E, F> func, U x) {
    try {
      func.accept(x);
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      throw wrapException(e);
    }
  }

  private <R, U> R handleFunction(ThrowingFunction<U, ? extends R, E, F> func, U x) {
    try {
      return func.apply(x);
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      throw wrapException(e);
    }
  }

  private <U> boolean handlePredicate(ThrowingPredicate<U, E, F> func, U x) {
    try {
      return func.test(x);
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      throw wrapException(e);
    }
  }

  /*
   * Terminal operations:
   */

  public CheckedOptional<T> min(Comparator<? super T> comparator) throws E, F {
    try {
      return CheckedOptional.from(stream.min(comparator));
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  public CheckedOptional<T> max(Comparator<? super T> comparator) throws E, F {
    try {
      return CheckedOptional.from(stream.max(comparator));
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  public <R, A> R collect(Collector<? super T, A, R> collector) throws E, F {
    try {
      return stream.collect(collector);
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  private RuntimeException wrapException(Exception e) {
    if(exceptionType.isInstance(e) || exceptionType2.isInstance(e)) {
      return new WrapperException(e);
    }

    return new IllegalStateException(e);  // Shouldn't actually occur as that means a different checked exception type was snuck in
  }

  @SuppressWarnings("unchecked")
  private <R> R throwException(WrapperException e) throws E, F {
    if(exceptionType.isInstance(e.getCause())) {
      throw (E)e.getCause();
    }

    throw (F)e.getCause();
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
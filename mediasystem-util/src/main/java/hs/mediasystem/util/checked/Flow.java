package hs.mediasystem.util.checked;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Wraps a standard {@link Stream} and allows handling of a checked exception.  Intermediate
 * operations all accept functions which throw a checked exception, while terminal
 * operations will declare the exception to be thrown.
 *
 * @param <T> the type of the {@link Stream}
 * @param <E1> the first type of exception to allow
 * @param <E2> the second type of exception to allow
 * @param <E3> the third type of exception to allow
 */
public class Flow<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {
  final Stream<T> stream;
  final Class<?>[] exceptionTypes;

  protected Flow(Stream<T> stream, Class<?>[] exceptionTypes) {
    if(exceptionTypes.length > 3) {
      throw new IllegalArgumentException("Only supports upto 3 checked exceptions: " + Arrays.toString(exceptionTypes));
    }

    this.stream = Objects.requireNonNull(stream);
    this.exceptionTypes = exceptionTypes;
  }

  /*
   * Intermediate operations:
   */

  public Flow<T, E1, E2, E3> filter(ThrowingPredicate<? super T, E1, E2, E3> predicate) {
    return new Flow<>(stream.filter(x -> handlePredicate(predicate, x)), exceptionTypes);
  }

  public <R> Flow<R, E1, E2, E3> flatMapStream(ThrowingFunction<? super T, ? extends Stream<? extends R>, E1, E2, E3> mapper) {
    return new Flow<>(stream.flatMap(x -> handleFunction(mapper, x)), exceptionTypes);
  }

  public <R> Flow<R, E1, E2, E3> flatMap(ThrowingFunction<? super T, ? extends Flow<? extends R, E1, E2, E3>, E1, E2, E3> mapper) {
    return new Flow<>(stream.flatMap(x -> handleFunction(mapper, x).stream), exceptionTypes);
  }

  public <R> Flow<R, E1, E2, E3> map(ThrowingFunction<? super T, ? extends R, E1, E2, E3> mapper) {
    return new Flow<>(stream.map(x -> handleFunction(mapper, x)), exceptionTypes);
  }

  public Flow<T, E1, E2, E3> peek(ThrowingConsumer<? super T, E1, E2, E3> action) {
    return new Flow<>(stream.peek(x -> handleConsumer(action, x)), exceptionTypes);
  }

  public void forEach(ThrowingConsumer<? super T, E1, E2, E3> action) {
    stream.forEach(x -> handleConsumer(action, x));
  }

  private <U> void handleConsumer(ThrowingConsumer<U, E1, E2, E3> func, U x) {
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

  private <R, U> R handleFunction(ThrowingFunction<U, ? extends R, E1, E2, E3> func, U x) {
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

  private <U> boolean handlePredicate(ThrowingPredicate<U, E1, E2, E3> func, U x) {
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

  public CheckedOptional<T> min(Comparator<? super T> comparator) throws E1, E2, E3 {
    try {
      return CheckedOptional.from(stream.min(comparator));
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  public CheckedOptional<T> max(Comparator<? super T> comparator) throws E1, E2, E3 {
    try {
      return CheckedOptional.from(stream.max(comparator));
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  public CheckedOptional<T> findFirst() throws E1, E2, E3 {
    try {
      return CheckedOptional.from(stream.findFirst());
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  public <R, A> R collect(Collector<? super T, A, R> collector) throws E1, E2, E3 {
    try {
      return stream.collect(collector);
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  public List<T> toList() throws E1, E2, E3 {
    try {
      return stream.toList();
    }
    catch(WrapperException e) {
      return throwException(e);
    }
  }

  private RuntimeException wrapException(Exception e) {
    for(Class<?> type : exceptionTypes) {
      if(type.isInstance(e)) {
        return new WrapperException(e);
      }
    }

    return new IllegalStateException(e);  // Shouldn't actually occur as that means a different checked exception type was snuck in
  }

  @SuppressWarnings("unchecked")
  private <R> R throwException(WrapperException e) throws E1, E2, E3 {
    if(exceptionTypes.length > 0 && exceptionTypes[0].isInstance(e.getCause())) {
      throw (E1)e.getCause();
    }
    if(exceptionTypes.length > 1 && exceptionTypes[1].isInstance(e.getCause())) {
      throw (E2)e.getCause();
    }
    if(exceptionTypes.length > 2 && exceptionTypes[2].isInstance(e.getCause())) {
      throw (E3)e.getCause();
    }

    throw new AssertionError("WrapperException contained exception that was not declared: " + e.getCause());
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
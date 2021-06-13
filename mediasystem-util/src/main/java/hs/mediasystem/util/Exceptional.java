package hs.mediasystem.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A container object similar to {@link Optional} which may or may not contain a non-null value
 * or contains a (caught) Exception. If a value is present, {@code isPresent()} will return {@code true} and
 * {@code get()} will return the value. If instead an exception is present, {@code get()} will throw an
 * {@link ExceptionalException}.<p>
 *
 * Various methods are provided to fluently deal with situations where the {@link Exceptional} is empty or
 * contains an exception.
 *
 * @author John Hendrikx
 * @param <T> the type contained within the {@link Exceptional}
 */
public final class Exceptional<T> {

  /*
   * The value and exception fields can be in three states:
   *
   * value != null && exception == null -> Exceptional contains a value
   * value == null && exception == null -> Exceptional is empty
   * value == null && exception != null -> Exceptional contains exception
   *
   * The state where both value and exception are not null is invalid
   */

  private final T value;
  private final Throwable exception;

  /**
   * Creates an empty {@link Exceptional}.
   *
   * @param <T> the type of the contained value
   * @return an empty {@link Exceptional}, never null
   */
  public static <T> Exceptional<T> empty() {
    return new Exceptional<>(null, null);
  }

  /**
   * Creates an {@link Exceptional} containing the given value, or an empty one if supplied value was null.
   *
   * @param <T> the type of the contained value
   * @param value the value this Exceptional will contain, can be null
   * @return an {@link Exceptional}, never null
   */
  public static <T> Exceptional<T> ofNullable(T value) {
    return value != null ? of(value) : empty();
  }

  /**
   * Creates an {@link Exceptional} containing the given value, or throws an exception if supplied value was null.
   *
   * @param <T> the type of the contained value
   * @param value the value this Exceptional will contain, cannot be null
   * @return an {@link Exceptional}, never null
   * @throws NullPointerException if supplied value was null
   */
  public static <T> Exceptional<T> of(T value) {
    return new Exceptional<>(Objects.requireNonNull(value), null);
  }

  public static <T> Exceptional<T> of(Optional<T> value) {
    return new Exceptional<>(Objects.requireNonNull(value).orElse(null), null);
  }

  /**
   * Creates an {@link Exceptional} containing the given exception, or an empty one if supplied exception was null.
   *
   * @param <T> the type of the contained value
   * @param exception the exception this Exceptional will contain, canot be null
   * @return an {@link Exceptional}, never null
   */
  public static <T> Exceptional<T> ofNullableException(Throwable exception) {
    return exception != null ? new Exceptional<>(null, exception) : empty();
  }

  /**
   * Creates an {@link Exceptional} containing the given exception, or throws an exception if supplied exception was null.
   *
   * @param <T> the type of the contained value
   * @param exception the exception this Exceptional will contain, canot be null
   * @return an {@link Exceptional}, never null
   * @throws NullPointerException if supplied exception was null
   */
  public static <T> Exceptional<T> ofException(Throwable exception) {
    return new Exceptional<>(null, Objects.requireNonNull(exception));
  }

  /**
   * Creates a new Exceptional with the value supplied by the given supplier.  If an
   * exception occurs in the supplier, it is caught and stored.  If the resulting
   * value is null, an empty Exceptional is returned.
   *
   * @param <T> the type of the contained value
   * @param supplier a supplier for the value of this Exceptional
   * @return a new Exceptional with the supplied value or a caught exception, never null
   */
  public static <T> Exceptional<T> from(TrySupplier<T> supplier) {
    try {
      return ofNullable(supplier.tryGet());
    }
    catch(Throwable t) {
      return new Exceptional<>(null, t);
    }
  }

  /**
   * Runs the given task, and returns an empty Exceptional if the task completed succesfully, otherwise
   * returns an Exceptional containing the thrown exception.
   *
   * @param task a task to run
   * @return an empty Exceptional or an Exceptional with a caught exception, never null
   */
  public static Exceptional<Void> fromVoid(TryRunnable task) {
    return from(() -> { task.run(); return null; });
  }

  private Exceptional(T value, Throwable exc) {
    this.value = value;
    this.exception = exc;
  }

  /**
   * Returns the contained exception, or throws {@link NoSuchElementException} if not present.
   *
   * @return the contained exception, never null
   * @throws NoSuchElementException if no exception was present
   */
  public Throwable getException() {
    if(exception != null) {
      return exception;
    }

    throw new NoSuchElementException("No exception present");
  }

  /**
   * Returns the value of this {@link Exceptional}, or throws {@link NoSuchElementException} if it contains
   * no exception, otherwise throws ExceptionalException with the contained exception.
   *
   * @return the value of this {@link Exceptional}, never null
   * @throws ExceptionalException if the Exceptional contained an exception
   * @throws NoSuchElementException if no value was present
   */
  public T get() {
    if(value != null) {
      return value;
    }
    if(exception != null) {
      throwExceptionalException(exception);
    }

    throw new NoSuchElementException("No value present");
  }

  /**
   * Returns the value of this {@link Exceptional} if present, or the supplied value if it contains no exception,
   * otherwise throws ExceptionalException with the contained exception.
   *
   * @param other an alternative value to return
   * @return the value of this {@link Exceptional} if present, or the supplied value if it contains no exception
   * @throws ExceptionalException if the Exceptional contained an exception
   */
  public T orElse(T other) {
    if(value != null) {
      return value;
    }
    if(exception != null) {
      throwExceptionalException(exception);
    }

    return other;
  }

  /**
   * Returns the value of this {@link Exceptional} if present, or the value supplied by the supplier if it
   * contains no exception, otherwise throws ExceptionalException with the contained exception.
   *
   * @param otherSupplier a {@link Supplier} which supplies an alternative value to return, cannot be null
   * @return the value of this {@link Exceptional} if present, or the supplied value if it contains no exception
   * @throws NullPointerException when otherSupplier is null
   * @throws ExceptionalException if the Exceptional contained an exception
   */
  public T orElseGet(Supplier<? extends T> otherSupplier) {
    Objects.requireNonNull(otherSupplier);

    if(value != null) {
      return value;
    }
    if(exception != null) {
      throwExceptionalException(exception);
    }

    return otherSupplier.get();
  }

  /**
   * Returns the value of this {@link Exceptional} if present, or throws the supplied exception if it contains
   * no exception, otherwise throws ExceptionalException with the contained exception.
   *
   * @param <X> the type of the supplied exception
   * @param exceptionSupplier a {@link Supplier} with an exception to throw, cannot be null
   * @return the value of this {@link Exceptional} if present, never null
   * @throws X when no value is present and the {@link Exceptional} did not contain an exception
   */
  public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
    Objects.requireNonNull(exceptionSupplier);

    if(value != null) {
      return value;
    }
    if(exception != null) {
      throwExceptionalException(exception);
    }

    throw exceptionSupplier.get();
  }

  /**
   * If a value is present, returns an {@code Exceptional} describing the value,
   * otherwise returns an {@code Exceptional} produced by the supplying function.
   *
   * @param supplier the supplying function that produces an {@code Exceptional}
   *        to be returned
   * @return returns an {@code Exceptional} describing the value of this
   *         {@code Exceptional}, if a value is present, otherwise an
   *         {@code Exceptional} produced by the supplying function.
   * @throws NullPointerException if the supplying function is {@code null} or
   *         produces a {@code null} result
   */
  public Exceptional<T> or(Supplier<? extends Exceptional<? extends T>> supplier) {
      Objects.requireNonNull(supplier);

      if (isPresent() || isException()) {
          return this;
      }

      @SuppressWarnings("unchecked")
      Exceptional<T> r = (Exceptional<T>) supplier.get();
      return Objects.requireNonNull(r);
  }

  /**
    * If a value is present, returns an {@code Exceptional} describing (as if by
    * {@link #ofNullable}) the result of applying the given mapping function to
    * the value, otherwise returns an empty {@code Exceptional}.
    *
    * <p>If the mapping function returns a {@code null} result then this method
    * returns an empty {@code Exceptional}.
    *
    * @param mapper the mapping function to apply to a value, if present
    * @param <U> The type of the value returned from the mapping function
    * @return an {@code Exceptional} describing the result of applying a mapping
    *         function to the value of this {@code Exceptional}, if a value is
    *         present, otherwise an empty {@code Exceptional}; if the mapping
    *         function throws an exception, returns an {@code Exceptional} with
    *         the exception.
    * @throws NullPointerException if the mapping function is {@code null}
    */
  public <U> Exceptional<U> map(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);

    if(value == null) {
      return new Exceptional<>(null, exception);
    }

    return from(() -> mapper.apply(value));
  }

  /**
   * If a value is present, returns the result of applying the given
   * {@code Exceptional}-bearing mapping function to the value, otherwise returns
   * this exceptional (empty or with exception).
   *
   * <p>This method is similar to {@link #map(Function)}, but the mapping
   * function is one whose result is already an {@code Exceptional}, and if
   * invoked, {@code flatMap} does not wrap it within an additional
   * {@code Exceptional}.
   *
   * <p>If the mapping function throws an exception, it is caught and an
   * {@link Exceptional} is returned containing it.
   *
   * @param <U> The type of value of the {@code Exceptional} returned by the
   *            mapping function
   * @param mapper the mapping function to apply to a value, if present
   * @return the result of applying an {@code Exceptional}-bearing mapping
   *         function to the value of this {@code Exceptional}, if a value is
   *         present, otherwise an empty {@code Exceptional}
   * @throws NullPointerException if the mapping function is {@code null} or
   *         returns a {@code null} result
   */
  @SuppressWarnings("unchecked")
  public <U> Exceptional<U> flatMap(Function<? super T, Exceptional<U>> mapper) {
    Objects.requireNonNull(mapper);

    Exceptional<U> exceptional;

    try {
        exceptional = value == null
            ? (Exceptional<U>)this // Cast here is safe, as this Exceptional does not contain a value
            : mapper.apply(value);
    }
    catch(Throwable t) {
        return Exceptional.ofException(t);
    }

    return Objects.requireNonNull(exceptional);
  }

  /**
   * Returns a {@link Stream} containing only the value of this Exceptional, or an empty
   * stream if the Exceptional is empty or contained an exception.
   *
   * @return a {@link Stream} containing 0 or 1 elements, never null
   */
  public Stream<T> ignoreAllAndStream() {
    return value == null ? Stream.empty() : Stream.of(value);
  }

  /**
   * Returns a {@link Stream} containing only the value of this Exceptional, or an empty
   * stream if the Exceptional is empty or throws an exception if it contained an exception.
   *
   * @return a {@link Stream} containing 0 or 1 elements, never null
   */
  public Stream<T> stream() {
    if(exception != null)  {
      throwExceptionalException(exception);
    }

    return value == null ? Stream.empty() : Stream.of(value);
  }

  /**
   * If a value is present, and the value matches the given predicate,
   * returns an {@code Exceptional} describing the value, otherwise returns the
   * current {@code Exceptional}.<p>
   *
   * If the filter function throws an exception, it is caught and stored into
   * a new {@link Exceptional}.
   *
   * @param predicate the predicate to apply to a value, if present
   * @return an {@code Exceptional} describing the value of this
   *         {@code Exceptional}, if a value is present and the value matches the
   *         given predicate, otherwise the current {@code Exceptional} or
   *         an {@link Exceptional} with any exception that occured.
   * @throws NullPointerException if the predicate is {@code null}
   */
  public Exceptional<T> filter(Predicate<? super T> predicate) {
    Objects.requireNonNull(predicate);

    if(value == null) {
      return this;
    }

    final boolean b;

    try {
      b = predicate.test(value);
    }
    catch(Throwable t) {
      return ofException(t);
    }

    return b ? this : empty();
  }

  /**
   * Ignores matching exception types by converting them into an empty {@link Exceptional},
   * otherwise returns this {@link Exceptional}.
   *
   * @param <X> the Throwable type
   * @param type the type of exception to ignore, cannot be null
   * @return an empty exceptional if it contained an exception matching the given type, otherwise this {@link Exceptional}
   */
  public <X extends Throwable> Exceptional<T> ignore(Class<? extends X> type) {
    Objects.requireNonNull(type);

    return type.isInstance(exception) ? empty() : this;
  }

  /**
   * Recovers from all exceptions by replacing it with the value supplied by a mapper,
   * otherwise returns this {@link Exceptional}.<p>
   *
   * If the mapping function throws an exception, it is caught and an {@link Exceptional} is returned containing it.
   *
   * @param mapper a mapper to supply a value for this {@link Exceptional}
   * @return a new {@link Exceptional} containing the value of the supplied mapper if it contained an exception, otherwise returns this {@link Exceptional}
   */
  public Exceptional<T> recover(Function<Throwable, T> mapper) {
    Objects.requireNonNull(mapper);

    return exception != null ? from(() -> mapper.apply(exception)) : this;
  }

  /**
   * Recovers from matching exception types by replacing it with the value supplied by a mapper,
   * otherwise returns this {@link Exceptional}.<p>
   *
   * If the mapping function throws an exception, it is caught and an {@link Exceptional} is returned containing it.
   *
   * @param <X> the Throwable type
   * @param type the type of exception to recover from, cannot be null
   * @param mapper a mapper to supply a value for this {@link Exceptional}
   * @return a new {@link Exceptional} containing the value of the supplied mapper if it contained a matching exception, otherwise returns this {@link Exceptional}
   */
  public <X extends Throwable> Exceptional<T> recover(Class<? extends X> type, Function<? super X, T> mapper) {
    Objects.requireNonNull(mapper);
    Objects.requireNonNull(type);

    return type.isInstance(exception) ? from(() -> mapper.apply(type.cast(exception))) : this;
  }

  /**
   * Recovers from matching exception types by replacing it with the value supplied by a mapper,
   * otherwise returns this {@link Exceptional}.
   *
   * If the mapping function throws an exception, it is caught and an {@link Exceptional} is returned containing it.
   *
   * @param <X> the Throwable type
   * @param types the types of exception to recover from
   * @param mapper a mapper to supply a value for this {@link Exceptional}
   * @return a new {@link Exceptional} containing the value of the supplied mapper if it contained a matching exception, otherwise returns this {@link Exceptional}
   */
  public <X extends Throwable> Exceptional<T> recover(Iterable<Class<? extends X>> types, Function<? super X, T> mapper) {
    Objects.requireNonNull(mapper);
    Objects.requireNonNull(types);

    for(Class<? extends X> type : types) {
      if(type.isInstance(exception)) {
        return from(() -> mapper.apply(type.cast(exception)));
      }
    }

    return this;
  }

  public <X extends Throwable> Exceptional<T> flatRecover(Class<? extends X> type, Function<? super X, Exceptional<T>> mapper) {
    Objects.requireNonNull(mapper);

    return type.isInstance(exception) ? Objects.requireNonNull(mapper.apply(type.cast(exception))) : this;
  }

  public <X extends Throwable> Exceptional<T> flatRecover(Iterable<Class<? extends X>> types, Function<? super X, Exceptional<T>> mapper) {
    Objects.requireNonNull(mapper);

    for(Class<? extends X> c : types) {
      if(c.isInstance(exception)) {
        return Objects.requireNonNull(mapper.apply(c.cast(exception)));
      }
    }

    return this;
  }

  public <E extends Throwable> Exceptional<T> propagate(Class<E> type) throws E {
    if(type.isInstance(exception)) {
      throw type.cast(exception);
    }

    return this;
  }

  public <E extends Throwable> Exceptional<T> propagate(Iterable<Class<? extends E>> types) throws E {
    for(Class<? extends E> type : types) {
      if(type.isInstance(exception)) {
        throw type.cast(exception);
      }
    }

    return this;
  }

  public <E extends Throwable, F extends Throwable> Exceptional<T> propagate(Class<E> type, Function<? super E, ? extends F> translator) throws F {
    if(type.isInstance(exception)) {
      throw translator.apply(type.cast(exception));
    }

    return this;
  }

  public <E extends Throwable, F extends Throwable> Exceptional<T> propagate(Iterable<Class<E>> types, Function<? super E, ? extends F> translator) throws F {
    for(Class<? extends E> type : types) {
      if(type.isInstance(exception)) {
        throw translator.apply(type.cast(exception));
      }
    }

    return this;
  }

  /**
   * Handles all exceptions by executing an action and converting it to an empty {@link Exceptional},
   * otherwise returns this {@link Exceptional}.<p>
   *
   * If the action throws an exception, it is caught and an {@link Exceptional} is returned containing it.
   *
   * @param action an action to execute, cannot be null
   * @return a new empty {@link Exceptional} if an exception was present, otherwise returns this {@link Exceptional} or an {@link Exceptional} containing the exception thrown by action
   */
  public Exceptional<T> handle(Consumer<Throwable> action) {
    Objects.requireNonNull(action);

    if(exception != null) {
      return from(() -> { action.accept(exception); return null; });
    }

    return this;
  }

  /**
   * Handles matching exception types by executing an action and converting it to an empty {@link Exceptional},
   * otherwise returns this {@link Exceptional}.<p>
   *
   * If the action throws an exception, it is caught and an {@link Exceptional} is returned containing it.
   *
   * @param <E> the Throwable type
   * @param type the type of exception to handle, cannot be null
   * @param action an action to execute, cannot be null
   * @return a new empty {@link Exceptional} if an exception was handled, otherwise returns this {@link Exceptional} or an {@link Exceptional} containing the exception thrown by action
   */
  public <E extends Throwable> Exceptional<T> handle(Class<? extends E> type, Consumer<? super E> action) {
    Objects.requireNonNull(type);
    Objects.requireNonNull(action);

    if(type.isInstance(exception)) {
      return from(() -> { action.accept(type.cast(exception)); return null; });
    }

    return this;
  }

  /**
   * Handles matching exception types by executing an action and converting it to an empty {@link Exceptional},
   * otherwise returns this {@link Exceptional}.<p>
   *
   * If the action throws an exception, it is caught and an {@link Exceptional} is returned containing it.
   *
   * @param <E> the Throwable type
   * @param types the types of exception to handle, cannot be null
   * @param action an action to execute, cannot be null
   * @return a new empty {@link Exceptional} if an exception was handled, otherwise returns this {@link Exceptional} or an {@link Exceptional} containing the exception thrown by action
   */
  public <E extends Throwable> Exceptional<T> handle(Iterable<Class<? extends E>> types, Consumer<? super E> action) {
    Objects.requireNonNull(types);
    Objects.requireNonNull(action);

    for(Class<? extends E> type : types) {
      if(type.isInstance(exception)) {
        return from(() -> { action.accept(type.cast(exception)); return null; });
      }
    }

    return this;
  }

  /**
   * Returns <code>true</code> if a non-null value is present, otherwise <code>false</code>.
   *
   * @return <code>true</code> if a non-null value is present, otherwise <code>false</code>.
   */
  public boolean isPresent() {
    return value != null;
  }

  /**
   * Returns <code>true</code> if a non-null value is present or there was an exception, otherwise <code>false</code>.
   *
   * @return <code>true</code> if a non-null value is present or there was an exception, otherwise <code>false</code>.
   */
  public boolean isNotEmpty() {
    return value != null || exception != null;
  }

  /**
   * If a value is present, performs the given action with the value,
   * or does nothing if there is no contained exception, otherwise throws
   * the contained exception.
   *
   * @param action the action to be performed, if a value is present
   * @throws NullPointerException if value is present and the given action is {@code null}
   */
  public void ifPresent(Consumer<? super T> action) {
    if(value != null) {
      action.accept(value);
    }
    if(exception != null) {
      throwExceptionalException(exception);
    }
  }

  /**
   * Returns <code>true</code> if an exception occured, otherwise <code>false</code>.
   *
   * @return <code>true</code> if an exception occured, otherwise <code>false</code>
   */
  public boolean isException() {
    return exception != null;
  }

  /**
   * Returns <code>true</code> if no exception occured, otherwise <code>false</code>.
   *
   * @return <code>true</code> if no exception occured, otherwise <code>false</code>
   */
  public boolean isNotException() {
    return exception == null;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Exceptional<?> other = (Exceptional<?>)obj;

    if(!Objects.equals(exception, other.exception)) {
      return false;
    }
    if(!Objects.equals(value, other.value)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, exception);
  }

  @Override
  public String toString() {
    if(exception != null) {
      return "Exceptional[" + exception + "]";
    }
    if(value != null) {
      return "Exceptional[" + value + "]";
    }

    return "Exceptional:empty";
  }

  private static void throwExceptionalException(Throwable t) {
    throw new ExceptionalException(t);
  }
}
package hs.mediasystem.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Exceptional<T> {
  private final T value;
  private final Throwable exception;

  private Exceptional(T value, Throwable exc) {
    this.value = value;
    this.exception = exc;
  }

  public static <T> Exceptional<T> empty() {
    return new Exceptional<>(null, null);
  }

  public static <T> Exceptional<T> ofNullable(T value) {
    return value != null ? of(value) : empty();
  }

  public static <T> Exceptional<T> of(T value) {
    return new Exceptional<>(Objects.requireNonNull(value), null);
  }

  public static <T> Exceptional<T> ofNullableException(Throwable exception) {
    return exception != null ? new Exceptional<>(null, exception) : empty();
  }

  public static <T> Exceptional<T> ofException(Throwable exception) {
    return new Exceptional<>(null, Objects.requireNonNull(exception));
  }

  /**
   * Creates a new Exceptional with the value supplied by the given supplier.  If an
   * exception occurs in the supplier, it is caught and stored.
   *
   * @param supplier a supplier for the value of this Exceptional
   * @return a new Exceptional with the supplied value or a caught exception
   */
  public static <T> Exceptional<T> from(TrySupplier<T> supplier) {
    try {
      return ofNullable(supplier.tryGet());
    }
    catch(Throwable t) {
      return new Exceptional<>(null, t);
    }
  }

  public static Exceptional<Void> fromVoid(TryRunnable task) {
    try {
      task.run();
      return new Exceptional<>(null, null);
    }
    catch (Throwable t) {
      return new Exceptional<>(null, t);
    }
  }

  public static <E extends Throwable> Consumer<? super E> swallow() {
    return e -> {};
  }

  public Throwable getException() {
    if(exception != null) {
      return exception;
    }

    throw new NoSuchElementException("No exception present");
  }

  public T get() {
    if(value != null) {
      return value;
    }
    if(exception != null) {
      sneakyThrow(exception);
    }

    throw new NoSuchElementException("No value present");
  }

  public T orElse(T other) {
    if(value != null) {
      return value;
    }
    if(exception != null) {
      sneakyThrow(exception);
    }

    return other;
  }

  /**
   * If a value is present, returns an {@code Optional} describing the value,
   * otherwise returns an {@code Optional} produced by the supplying function.
   *
   * @param supplier the supplying function that produces an {@code Optional}
   *        to be returned
   * @return returns an {@code Optional} describing the value of this
   *         {@code Optional}, if a value is present, otherwise an
   *         {@code Optional} produced by the supplying function.
   * @throws NullPointerException if the supplying function is {@code null} or
   *         produces a {@code null} result
   * @since 9
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

  public T orElseGet(Supplier<? extends T> other) {
    if(value != null) {
      return value;
    }
    if(exception != null) {
      sneakyThrow(exception);
    }

    return other.get();
  }

  public <U> Exceptional<U> map(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);

    if(value == null) {
      return new Exceptional<>(null, exception);
    }

    try {
      return ofNullable(mapper.apply(value));
    }
    catch(Throwable t) {
      return new Exceptional<>(null, t);
    }
  }

  public <U> Exceptional<U> flatMap(Function<? super T, Exceptional<U>> mapper) {
    Objects.requireNonNull(mapper);
    return value != null ? Objects.requireNonNull(mapper.apply(value)) : (Exceptional<U>)this;  // TODO I get the impression this should be empty() but this, or exception is swallowed.
  }

  public Stream<T> ignoreAllAndStream() {
    return value == null ? Stream.empty() : Stream.of(value);
  }

  public Stream<T> stream() {
    if(exception != null)  {
      sneakyThrow(exception);
    }

    return value == null ? Stream.empty() : Stream.of(value);
  }

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

  public <X extends Throwable> Exceptional<T> ignore(Class<? extends X> excType) {
    return excType.isInstance(exception) ? empty() : this;
  }

  public <X extends Throwable> Exceptional<T> recover(Class<? extends X> excType, Function<? super X, T> mapper) {
    Objects.requireNonNull(mapper);
    return excType.isInstance(exception) ? ofNullable(mapper.apply(excType.cast(exception))) : this;
  }

  public <X extends Throwable> Exceptional<T> recover(Iterable<Class<? extends X>> excTypes, Function<? super X, T> mapper) {
    Objects.requireNonNull(mapper);
    for (Class<? extends X> excType : excTypes) {
      if (excType.isInstance(exception)) {
        return ofNullable(mapper.apply(excType.cast(exception)));
      }
    }
    return this;
  }

  public <X extends Throwable> Exceptional<T> flatRecover(Class<? extends X> excType, Function<? super X, Exceptional<T>> mapper) {
    Objects.requireNonNull(mapper);
    return excType.isInstance(exception) ? Objects.requireNonNull(mapper.apply(excType.cast(exception))) : this;
  }

  public <X extends Throwable> Exceptional<T> flatRecover(Iterable<Class<? extends X>> excTypes, Function<? super X, Exceptional<T>> mapper) {
    Objects.requireNonNull(mapper);
    for(Class<? extends X> c : excTypes) {
      if(c.isInstance(exception)) {
        return Objects.requireNonNull(mapper.apply(c.cast(exception)));
      }
    }
    return this;
  }

  public <E extends Throwable> Exceptional<T> propagate(Class<E> excType) throws E {
    if (excType.isInstance(exception)) {
      throw excType.cast(exception);
    }

    return this;
  }

  public <E extends Throwable> Exceptional<T> propagate(Iterable<Class<? extends E>> excTypes) throws E {
    for(Class<? extends E> excType : excTypes) {
      if(excType.isInstance(exception)) {
        throw excType.cast(exception);
      }
    }

    return this;
  }

  public <E extends Throwable, F extends Throwable> Exceptional<T> propagate(Class<E> excType, Function<? super E, ? extends F> translator) throws F {
    if(excType.isInstance(exception)) {
      throw translator.apply(excType.cast(exception));
    }

    return this;
  }

  public <E extends Throwable, F extends Throwable> Exceptional<T> propagate(Iterable<Class<E>> excTypes, Function<? super E, ? extends F> translator) throws F {
    for(Class<? extends E> excType : excTypes) {
      if(excType.isInstance(exception)) {
        throw translator.apply(excType.cast(exception));
      }
    }

    return this;
  }

  public Exceptional<T> handle(Consumer<Throwable> action) {
    if (exception != null) {
      action.accept(Throwable.class.cast(exception));
      return empty();
    }
    return this;
  }

  public <E extends Throwable> Exceptional<T> handle(Class<E> excType, Consumer<? super E> action) {
    if (excType.isInstance(exception)) {
      action.accept(excType.cast(exception));
      return empty();
    }
    return this;
  }

  public <E extends Throwable> Exceptional<T> handle(Iterable<Class<E>> excTypes, Consumer<? super E> action) {
    for (Class<? extends E> excType : excTypes)
      if (excType.isInstance(exception)) {
        action.accept(excType.cast(exception));
        return empty();
      }
    return this;
  }

  public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
    if (value != null) return value;
    if (exception != null) sneakyThrow(exception);
    throw exceptionSupplier.get();
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

  public void ifPresent(Consumer<? super T> consumer) {
    if(value != null) {
      consumer.accept(value);
    }
    if(exception != null) {
      sneakyThrow(exception);
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
    return Objects.hashCode(value);
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

  private static void sneakyThrow(Throwable t) {
    throw new ExceptionalException(t);
  }
}
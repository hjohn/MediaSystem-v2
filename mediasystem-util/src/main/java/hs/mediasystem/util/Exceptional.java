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
    return exception != null? new Exceptional<>(null, exception) : empty();
  }

  public static <T> Exceptional<T> ofException(Throwable exception) {
    return new Exceptional<>(null, Objects.requireNonNull(exception));
  }

  public static <T> Exceptional<T> from(TrySupplier<T> supplier) {
    try {
      return ofNullable(supplier.tryGet());
    } catch (Throwable t) {
      return new Exceptional<>(null, t);
    }
  }

  public static Exceptional<Void> fromVoid(TryRunnable task) {
    try {
      task.run();
      return new Exceptional<>(null, null);
    } catch (Throwable t) {
      return new Exceptional<>(null, t);
    }
  }

  public static <E extends Throwable> Consumer<? super E> swallow() {
    return e -> {};
  }

  public T get() {
    if (value != null) return value;
    if (exception != null) sneakyThrow(exception);
    throw new NoSuchElementException("No value present");
  }

  public T orElse(T other) {
    if (value != null) return value;
    if (exception != null) sneakyThrow(exception);
    return other;
  }

  public T orElseGet(Supplier<? extends T> other) {
    if (value != null) return value;
    if (exception != null) sneakyThrow(exception);
    return other.get();
  }

  public<U> Exceptional<U> map(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    if (value == null) return new Exceptional<>(null, exception);
    final U u;
    try {
      u = mapper.apply(value);
    } catch (Throwable exc) {
      return new Exceptional<>(null, exc);
    }
    return ofNullable(u);
  }

  public<U> Exceptional<U> flatMap(Function<? super T, Exceptional<U>> mapper) {
    Objects.requireNonNull(mapper);
    return value != null ? Objects.requireNonNull(mapper.apply(value)) : empty();
  }

  public Stream<T> stream() {
    return value == null ? Stream.empty() : Stream.of(value);
  }

  public Exceptional<T> filter(Predicate<? super T> predicate) {
    Objects.requireNonNull(predicate);
    if (value == null) return this;
    final boolean b;
    try {
      b = predicate.test(value);
    } catch (Throwable t) {
      return ofException(t);
    }
    return b ? this : empty();
  }

  public <X extends Throwable> Exceptional<T> ignore(Class<? extends X> excType) {
    return excType.isInstance(exception) ? ofNullable(null) : this;
  }

  public <X extends Throwable> Exceptional<T> recover(
      Class<? extends X> excType, Function<? super X, T> mapper)
  {
    Objects.requireNonNull(mapper);
    return excType.isInstance(exception) ? ofNullable(mapper.apply(excType.cast(exception))) : this;
  }

  public <X extends Throwable> Exceptional<T> recover(
      Iterable<Class<? extends X>> excTypes, Function<? super X, T> mapper)
  {
    Objects.requireNonNull(mapper);
    for (Class<? extends X> excType : excTypes)
      if (excType.isInstance(exception))
        return ofNullable(mapper.apply(excType.cast(exception)));
    return this;
  }

  public <X extends Throwable> Exceptional<T> flatRecover(
      Class<? extends X> excType, Function<? super X, Exceptional<T>> mapper)
  {
    Objects.requireNonNull(mapper);
    return excType.isInstance(exception) ? Objects.requireNonNull(mapper.apply(excType.cast(exception))) : this;
  }

  public <X extends Throwable> Exceptional<T> flatRecover(
      Iterable<Class<? extends X>> excTypes, Function<? super X, Exceptional<T>> mapper)
  {
    Objects.requireNonNull(mapper);
    for (Class<? extends X> c : excTypes)
      if (c.isInstance(exception))
        return Objects.requireNonNull(mapper.apply(c.cast(exception)));
    return this;
  }

  public <E extends Throwable> Exceptional<T> propagate(Class<E> excType) throws E {
    if (excType.isInstance(exception))
      throw excType.cast(exception);
    return this;
  }

  public <E extends Throwable> Exceptional<T> propagate(Iterable<Class<? extends E>> excTypes) throws E {
    for (Class<? extends E> excType : excTypes)
      if (excType.isInstance(exception))
        throw excType.cast(exception);
    return this;
  }

  public <E extends Throwable, F extends Throwable> Exceptional<T> propagate(
      Class<E> excType, Function<? super E, ? extends F> translator)
  throws F
  {
    if (excType.isInstance(exception))
      throw translator.apply(excType.cast(exception));
    return this;
  }

  public <E extends Throwable, F extends Throwable> Exceptional<T> propagate(
      Iterable<Class<E>> excTypes, Function<? super E, ? extends F> translator)
  throws F
  {
    for (Class<? extends E> excType : excTypes)
      if (excType.isInstance(exception))
        throw translator.apply(excType.cast(exception));
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

  public boolean isPresent() {
    return value != null;
  }

  public void ifPresent(Consumer<? super T> consumer) {
    if (value != null)
      consumer.accept(value);
    if (exception != null) sneakyThrow(exception);
  }

  public boolean isException() {
    return exception != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    return obj instanceof Exceptional && Objects.equals(value, ((Exceptional)obj).value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }
}
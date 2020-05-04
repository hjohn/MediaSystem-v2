package hs.mediasystem.util.checked;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

public class CheckedOptional<T> {
  private static final CheckedOptional<?> EMPTY = new CheckedOptional<>(null);

  @SuppressWarnings("unchecked")
  public static <T> CheckedOptional<T> empty() {
    return (CheckedOptional<T>)EMPTY;
  }

  public static <T> CheckedOptional<T> of(T value) {
    return new CheckedOptional<>(value);
  }

  public static <T> CheckedOptional<T> ofNullable(T value) {
    return value == null ? empty() : of(value);
  }

  private final T value;

  private CheckedOptional(T value) {
    this.value = value;
  }

  public <U, E extends Exception> CheckedOptional<U> map(ThrowingFunction<? super T, ? extends U, E> mapper) throws E {
    Objects.requireNonNull(mapper);

    if(!isPresent()) {
      return empty();
    }

    return CheckedOptional.ofNullable(mapper.apply(value));
  }

  @SuppressWarnings("unchecked")
  public <U, E extends Exception> CheckedOptional<U> flatMap(ThrowingFunction<? super T, ? extends CheckedOptional<? extends U>, E> mapper) throws E {
    Objects.requireNonNull(mapper);

    if(!isPresent()) {
      return empty();
    }

    return Objects.requireNonNull((CheckedOptional<U>)mapper.apply(value));
  }

  @SuppressWarnings("unchecked")
  public <E extends Exception> CheckedOptional<T> or(ThrowingSupplier<? extends CheckedOptional<? extends T>, E> supplier) throws E {
    Objects.requireNonNull(supplier);

    if(isPresent()) {
      return this;
    }

    return Objects.requireNonNull((CheckedOptional<T>)supplier.get());
  }

  public T orElseThrow() {
    if(value == null) {
      throw new NoSuchElementException("No value present");
    }

    return value;
  }

  public Optional<T> toOptional() {
    return Optional.ofNullable(value);
  }

  public static <U> CheckedOptional<U> from(Optional<U> optional) {
    return new CheckedOptional<>(optional.isPresent() ? optional.orElseThrow() : null);
  }

  public boolean isPresent() {
    return value != null;
  }
}

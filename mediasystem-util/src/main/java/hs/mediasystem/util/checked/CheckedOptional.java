package hs.mediasystem.util.checked;

import java.util.List;
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

  public static <U> CheckedOptional<U> from(Optional<U> optional) {
    return new CheckedOptional<>(optional.isPresent() ? optional.orElseThrow() : null);
  }

  private final T value;

  private CheckedOptional(T value) {
    this.value = value;
  }

  public <E1 extends Exception, E2 extends Exception, E3 extends Exception> CheckedOptional<T> filter(ThrowingPredicate<? super T, E1, E2, E3> predicate) throws E1, E2, E3 {
    Objects.requireNonNull(predicate);

    return isPresent() && predicate.test(value) ? this : empty();
  }

  public <U, E1 extends Exception, E2 extends Exception, E3 extends Exception> CheckedOptional<U> map(ThrowingFunction<? super T, ? extends U, E1, E2, E3> mapper) throws E1, E2, E3 {
    Objects.requireNonNull(mapper);

    if(!isPresent()) {
      return empty();
    }

    return CheckedOptional.ofNullable(mapper.apply(value));
  }

  @SuppressWarnings("unchecked")
  public <U, E1 extends Exception, E2 extends Exception, E3 extends Exception> CheckedOptional<U> flatMap(ThrowingFunction<? super T, ? extends CheckedOptional<? extends U>, E1, E2, E3> mapper) throws E1, E2, E3 {
    Objects.requireNonNull(mapper);

    if(!isPresent()) {
      return empty();
    }

    return Objects.requireNonNull((CheckedOptional<U>)mapper.apply(value));
  }

  @SuppressWarnings("unchecked")
  public <U, E1 extends Exception, E2 extends Exception, E3 extends Exception> CheckedOptional<U> flatMapOpt(ThrowingFunction<? super T, ? extends Optional<? extends U>, E1, E2, E3> mapper) throws E1, E2, E3 {
    Objects.requireNonNull(mapper);

    if(!isPresent()) {
      return empty();
    }

    return CheckedOptional.from(Objects.requireNonNull((Optional<U>)mapper.apply(value)));
  }

  @SuppressWarnings("unchecked")
  public <E1 extends Exception, E2 extends Exception, E3 extends Exception> CheckedOptional<T> or(ThrowingSupplier<? extends CheckedOptional<? extends T>, E1, E2, E3> supplier) throws E1, E2, E3 {
    Objects.requireNonNull(supplier);

    if(isPresent()) {
      return this;
    }

    return Objects.requireNonNull((CheckedOptional<T>)supplier.get());
  }

  public <E1 extends Exception, E2 extends Exception, E3 extends Exception> T orElseGet(ThrowingSupplier<? extends T, E1, E2, E3> supplier) throws E1, E2, E3 {
    return isPresent() ? value : supplier.get();
  }

  public T orElse(T other) {
    return isPresent() ? value : other;
  }

  public T orElseThrow() {
    if(value == null) {
      throw new NoSuchElementException("No value present");
    }

    return value;
  }

  public Stream0<T> stream() {
    return isPresent() ? CheckedStreams.of(List.of(value)) : CheckedStreams.of(List.of());
  }

  public Optional<T> toOptional() {
    return Optional.ofNullable(value);
  }

  public boolean isPresent() {
    return value != null;
  }
}

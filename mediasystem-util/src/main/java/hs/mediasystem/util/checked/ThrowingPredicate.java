package hs.mediasystem.util.checked;

public interface ThrowingPredicate<T, E extends Exception> {
  boolean test(T t) throws E;
}

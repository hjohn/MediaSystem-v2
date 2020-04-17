package hs.mediasystem.util.checked;

public interface ThrowingFunction<T, R, E extends Exception> {
  R apply(T t) throws E;
}
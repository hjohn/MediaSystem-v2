package hs.mediasystem.util.checked;

public interface ThrowingFunction<T, R, E1 extends Exception, E2 extends Exception, E3 extends Exception> {
  R apply(T t) throws E1, E2, E3;
}
package hs.mediasystem.util.checked;

public interface ThrowingPredicate<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {
  boolean test(T t) throws E1, E2, E3;
}

package hs.mediasystem.util.checked;

public interface ThrowingPredicate<T, E extends Exception, F extends Exception> {
  boolean test(T t) throws E, F;
}

package hs.mediasystem.util.checked;

public interface ThrowingSupplier<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {
  T get() throws E1, E2, E3;
}

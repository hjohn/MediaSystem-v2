package hs.mediasystem.util.checked;

public interface ThrowingSupplier<T, E extends Exception> {
  T get() throws E;
}

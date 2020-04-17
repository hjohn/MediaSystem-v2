package hs.mediasystem.util.checked;

public interface ThrowingConsumer<T, E extends Exception> {
  void accept(T t) throws E;
}

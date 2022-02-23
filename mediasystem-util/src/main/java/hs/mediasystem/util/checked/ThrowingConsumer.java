package hs.mediasystem.util.checked;

public interface ThrowingConsumer<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> {
  void accept(T t) throws E1, E2, E3;
}

package hs.mediasystem.util.checked;

import java.util.Collection;
import java.util.stream.Stream;

public class Stream0<T> extends Flow<T, RuntimeException, RuntimeException, RuntimeException> {

  public static <T> Stream0<T> of(Stream<T> stream) {
    return new Stream0<>(stream);
  }

  public static <T> Stream0<T> of(Collection<T> collection) {
    return new Stream0<>(collection.stream());
  }

  protected Stream0(Stream<T> stream) {
    super(stream, new Class<?>[] {});
  }

  public <X extends Exception> Stream1<T, X> declaring(Class<X> exceptionType) {
    return new Stream1<>(stream, exceptionType);
  }

}

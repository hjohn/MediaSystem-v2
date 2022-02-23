package hs.mediasystem.util.checked;

import java.util.stream.Stream;

public class Stream1<T, E extends Exception> extends Flow<T, E, E, E> {

  protected Stream1(Stream<T> stream, Class<E> exceptionType) {
    super(stream, new Class<?>[] {exceptionType});
  }

  @SuppressWarnings("unchecked")
  <X extends Exception> Stream2<T, E, X> declaring(Class<X> exceptionType) {
    return new Stream2<>(stream, (Class<E>)exceptionTypes[0], exceptionType);
  }

}

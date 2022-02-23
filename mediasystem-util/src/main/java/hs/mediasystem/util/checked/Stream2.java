package hs.mediasystem.util.checked;

import java.util.stream.Stream;

public class Stream2<T, E1 extends Exception, E2 extends Exception> extends Flow<T, E1, E2, E2> {

  protected Stream2(Stream<T> stream, Class<E1> exceptionType1, Class<E2> exceptionType2) {
    super(stream, new Class<?>[] {exceptionType1, exceptionType2});
  }

  @SuppressWarnings("unchecked")
  <X extends Exception> Stream3<T, E1, E2, X> declaring(Class<X> exceptionType) {
    return new Stream3<>(stream, (Class<E1>)exceptionTypes[0], (Class<E2>)exceptionTypes[1], exceptionType);
  }
}
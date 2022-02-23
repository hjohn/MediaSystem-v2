package hs.mediasystem.util.checked;

import java.util.stream.Stream;

public class Stream3<T, E1 extends Exception, E2 extends Exception, E3 extends Exception> extends Flow<T, E1, E2, E3> {

  protected Stream3(Stream<T> stream, Class<E1> exceptionType1, Class<E2> exceptionType2, Class<E3> exceptionType3) {
    super(stream, new Class<?>[] {exceptionType1, exceptionType2, exceptionType3});
  }
}


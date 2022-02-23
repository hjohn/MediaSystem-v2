package hs.mediasystem.util.checked;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

public class CheckedStreams {

  public static <T> Stream0<T> of(Collection<T> collection) {
    return new Stream0<>(collection.stream());
  }

  public static <T> Stream1<T, IOException> forIOException(Collection<T> collection) {
    return new Stream1<>(collection.stream(), IOException.class);
  }

  public static <T> Stream0<T> of(Stream<T> stream) {
    return new Stream0<>(stream);
  }

  public static <T> Stream1<T, IOException> forIOException(Stream<T> stream) {
    return new Stream1<>(stream, IOException.class);
  }
}

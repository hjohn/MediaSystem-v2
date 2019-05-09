package hs.mediasystem.util;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Guards data with a lock.  The data can be read or manipulated with functions.
 * Care must be taken not to return the actual data directly, as it can then be
 * accessed without the guard's protection.<p>
 *
 * This class helps to not accidently access the protected data directly by
 * enforcing the use of read/write functions.
 *
 * @param <T>
 */
public class Guard<T> {
  private final ReentrantLock lock = new ReentrantLock();
  private final T data;

  public Guard(T data) {
    if(data == null) {
      throw new IllegalArgumentException("data cannot be null");
    }

    this.data = data;
  }

  public void write(Consumer<T> consumer) {
    lock.lock();

    try {
      consumer.accept(data);
    }
    finally {
      lock.unlock();
    }
  }

  public void execute(Consumer<T> consumer) {
    write(consumer);
  }

  public <R> R read(Function<T, R> consumer) {
    lock.lock();

    try {
      return consumer.apply(data);
    }
    finally {
      lock.unlock();
    }
  }
}
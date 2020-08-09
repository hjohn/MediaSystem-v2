package hs.database.util;

public interface UncheckedCloseable extends Runnable, AutoCloseable {
  @Override
  default void run() {
    try {
      close();
    }
    catch(Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  static UncheckedCloseable empty() {
    return wrap(() -> {});
  }

  static UncheckedCloseable wrap(AutoCloseable c) {
    return c::close;
  }

  default UncheckedCloseable nest(AutoCloseable c) {
    return () -> {
      try(UncheckedCloseable c1 = this) {
        c.close();
      }
    };
  }
}
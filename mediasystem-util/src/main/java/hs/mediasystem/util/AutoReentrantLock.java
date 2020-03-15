package hs.mediasystem.util;

import java.util.concurrent.locks.ReentrantLock;

public class AutoReentrantLock {
  private final ReentrantLock reentrantLock = new ReentrantLock();

  /**
   * Obtains the lock, returning a {@link Key} that can be used to
   * release the lock again (early) and/or automatically release it when
   * used in a try-with-resource block.
   *
   * @return a {@link Key}, never null
   */
  public Key lock() {
    return new Key();
  }

  public class Key implements AutoCloseable {
    private boolean alreadyClosed;

    private Key() {
      reentrantLock.lock();
    }

    public void earlyUnlock() {
      close();
    }

    @Override
    public void close() {
      if(!alreadyClosed) {
        reentrantLock.unlock();
        alreadyClosed = true;
      }
    }
  }
}

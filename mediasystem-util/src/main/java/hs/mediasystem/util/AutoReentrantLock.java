package hs.mediasystem.util;

import java.util.concurrent.locks.ReentrantLock;

public class AutoReentrantLock implements AutoCloseable {
  private final ReentrantLock lock;

  public AutoReentrantLock() {
    this.lock = new ReentrantLock();
  }

  public AutoReentrantLock lock() {
    lock.lock();
    return this;
  }

  @Override
  public void close() {
    lock.unlock();
  }
}

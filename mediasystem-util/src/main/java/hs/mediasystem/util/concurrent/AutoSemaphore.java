package hs.mediasystem.util.concurrent;

import hs.mediasystem.util.checked.ThrowingSupplier;

import java.util.concurrent.Semaphore;

public class AutoSemaphore extends Semaphore {

  public AutoSemaphore(int permits) {
    super(permits);
  }

  public <T, E1 extends Exception, E2 extends Exception, E3 extends Exception> T execute(ThrowingSupplier<T, E1, E2, E3> supplier) throws InterruptedException, E1, E2, E3 {
    acquire();

    try {
      return supplier.get();
    }
    finally {
      release();
    }
  }
}

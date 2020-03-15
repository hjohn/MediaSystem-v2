package hs.mediasystem.util;

public class RateLimiter {
  private static final long NANOSECONDS_PER_SECOND = 1000L * 1000L * 1000L;
  private static final long NANOSECONDS_PER_MILLISECOND = 1000L * 1000L;

  private final double permitsPerNanoSecond;
  private final double maxPermits;

  private double availablePermits;
  private long lastNanos = System.nanoTime();

  public RateLimiter(double permits, double perSeconds) {
    this.permitsPerNanoSecond = permits / perSeconds / NANOSECONDS_PER_SECOND;
    this.maxPermits = permits;
  }

  public RateLimiter(double permitsPerSecond) {
    this(permitsPerSecond, 1.0);
  }

  public synchronized void acquire() {
    updatePermits();

    while(availablePermits < 1.0) {
      try {
        // ISSUE: when multiple threads are waiting, some threads might starve, especially when they acquire different amounts of permits (NYI)

        // NOTE: sleep does not release the synchronized monitor here, so there is actually only one thread sleeping in this loop, while the rest will be blocked at the start of the method.

        // Sleep roughly the amount of time it would take for a permit to become available, or a minimum of 1 ms:
        Thread.sleep(Math.min(1L, (long)((1.0 - availablePermits) / permitsPerNanoSecond * NANOSECONDS_PER_MILLISECOND)));
      }
      catch(InterruptedException e) {
        // ignore
      }

      updatePermits();
    }

    availablePermits -= 1.0;
  }

  private synchronized void updatePermits() {
    long currentNanos = System.nanoTime();
    long nanosElapsed = currentNanos - lastNanos;

    availablePermits += nanosElapsed * permitsPerNanoSecond;
    lastNanos = currentNanos;

    if(availablePermits > maxPermits) {
      availablePermits = maxPermits;
    }
  }
}

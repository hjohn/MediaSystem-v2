package hs.mediasystem.util;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityRateLimiter {
  private static final long NANOSECONDS_PER_SECOND = 1000L * 1000L * 1000L;
  private static final long NANOSECONDS_PER_MILLISECOND = 1000L * 1000L;

  private final double permitsPerNanoSecond;
  private final double maxPermits;

  private double availablePermits;
  private long lastNanos = System.nanoTime();

  private final PriorityBlockingQueue<ThreadWrapper> queue = new PriorityBlockingQueue<>(11, Comparator.comparing(ThreadWrapper::getPriority).reversed());
  private final Lock queueLock = new ReentrantLock();
  private final Object permitsLock = new Object();

  /**
   * Holds the thread currently waiting for permits to become available (by using
   * measured sleeps).  This isn't necessarily the current highest priority thread,
   * but one that was at the head of the queue at some point.
   */
  private ThreadWrapper currentWaitingThread;

  /**
   * Constructs a new instance which allows the given amount of permits to be used
   * in the given amount of seconds.<p>
   *
   * Note that there is a difference between 1 permit per 1 second and 5 permits
   * per 5 seconds.  In the first case, the permission rate is fixed at 1 per second
   * in all cases, while in the second case, 5 permissions can be given instantly
   * before the rate is limited to 1 per second (or 5 per 5 seconds).
   *
   * @param maxBurstPermits the maximum amount of permits that can be "stored" up for bursting
   * @param perSeconds the time it takes all permits (= maxBurstPermits) to be refreshed
   */
  public PriorityRateLimiter(double maxBurstPermits, double perSeconds) {
    this.permitsPerNanoSecond = maxBurstPermits / perSeconds / NANOSECONDS_PER_SECOND;
    this.maxPermits = maxBurstPermits;
  }

  public PriorityRateLimiter(double permitsPerSecond) {
    this(permitsPerSecond, 1.0);
  }

  public void acquire() {
    for(;;) {
      ThreadWrapper currentThread = new ThreadWrapper(Thread.currentThread());

      queueLock.lock();

      if(currentWaitingThread == null) {
        currentWaitingThread = currentThread;

        queueLock.unlock();
      }
      else {
        queue.add(currentThread);

        synchronized(currentThread) {
          queueLock.unlock();  // Must be unlocked *after* acquiring thread wrapper monitor

          waitForNotification(currentThread);
        }
      }

      // Synchronized because permits are altered in two places, potentially by two different threads at once:
      synchronized(permitsLock) {
        waitForSufficientPermits();
      }

      /*
       * Sufficient permits are now available.  This thread must either consume
       * them itself, or allow another thread to consume them (if one is available
       * with higher priority) and put itself back into the wait queue.
       */

      queueLock.lock();

      try {
        currentWaitingThread = queue.poll();

        if(currentWaitingThread != null) {
          synchronized(currentWaitingThread) {
            currentWaitingThread.notify();
          }

          // The thread that was waiting for permits may not be the highest priority one...
          if(currentWaitingThread.getPriority() > currentThread.getPriority()) {
            continue;  // ... it wasn't, back into the queue it goes!

            // Note: The higher priority thread that was awakened won't have to wait as the permits are already available and not consumed yet.
          }
        }

        synchronized(permitsLock) {
          // Current thread was the highest priority one, consume the permits, and allow thread to continue:
          availablePermits -= 1.0;
          break;
        }
      }
      finally {
        queueLock.unlock();
      }
    }
  }

  private void waitForSufficientPermits() {
    updatePermits();

    while(availablePermits < 1.0) {
      try {
        // Sleep roughly the amount of time it would take for a permit to become available, or a minimum of 1 ms:
        Thread.sleep(Math.min(1L, (long)((1.0 - availablePermits) / permitsPerNanoSecond * NANOSECONDS_PER_MILLISECOND)));
      }
      catch(InterruptedException e) {
        // ignore
      }

      updatePermits();
    }
  }

  private void waitForNotification(ThreadWrapper currentThread) {
    for(;;) {
      try {
        currentThread.wait();

        queueLock.lock();

        try {
          if(currentWaitingThread == currentThread) {
            break;
          }
        }
        finally {
          queueLock.unlock();
        }
      }
      catch(InterruptedException e) {
        // Ignore
      }
    }
  }

  private void updatePermits() {
    long currentNanos = System.nanoTime();
    long nanosElapsed = currentNanos - lastNanos;

    availablePermits += nanosElapsed * permitsPerNanoSecond;
    lastNanos = currentNanos;

    if(availablePermits > maxPermits) {
      availablePermits = maxPermits;
    }
  }

  /**
   * Wrapper class required for {@link Thread}, because synchronizing directly on threads
   * breaks the notify/wait system used above (unknown why, perhaps something else is
   * synchronizing on threads or notifying them, or perhaps threads are just "special").<p>
   *
   * A possibility why this fails is that others may call {@link Thread#join()}, which
   * synchronizes on the Thread, which would result in not being able to exit {@link Object#wait()} as
   * someone else is holding the monitor.  That is certainly the case in the unit tests where
   * the test must wait for threads to finish before asserting results.
   */
  private static class ThreadWrapper {
    final Thread thread;

    ThreadWrapper(Thread thread) {
      this.thread = thread;
    }

    int getPriority() {
      return this.thread.getPriority();
    }
  }
}

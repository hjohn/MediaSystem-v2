package hs.mediasystem.util;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityRateLimiter {
  private static final long NANOSECONDS_PER_SECOND = 1000L * 1000L * 1000L;

  private final double permitsPerNanoSecond;
  private final double maxPermits;

  private double availablePermits;  // only access while holding permits lock
  private long lastNanos = System.nanoTime();  // only access while holding permits lock

  private final PriorityBlockingQueue<ThreadWrapper> queue = new PriorityBlockingQueue<>(11, Comparator.comparing(ThreadWrapper::getPriority).reversed());
  private final Lock queueLock = new ReentrantLock(true);
  private final Condition queueWaitCondition = queueLock.newCondition();

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

  public void acquire() throws InterruptedException {
    ThreadWrapper currentThread = new ThreadWrapper(Thread.currentThread(), queueLock.newCondition());

    for(;;) {
      queueLock.lock();

      try {
        if(currentWaitingThread == null) {
          currentWaitingThread = currentThread;
        }
        else {
          queue.add(currentThread);

          waitForNotification(currentThread);  // will release (and re-aquire) queueLock while blocking
        }

        /*
         * Only one thread should ever be in this part, as they're either the current waiting
         * thread, or they're not (in which case they're waiting to be notified).
         */

        waitForSufficientPermits();  // will release (and re-aquire) queueLock while blocking

        /*
         * Sufficient permits are now available.  This thread must either consume
         * them itself, or allow another thread to consume them (if one is available
         * with higher priority) and put itself back into the wait queue.
         *
         * In all cases, we take the top thread and wake it.
         * - If the awakened thread is higher priority, it will see there are enough permits and consume them
         *   -> The current thread is requeue'd (at the end unfortunately)
         * - If the awakened thread was lower priority it becomes the new waiting thread
         *   -> The current thread consumes the permits
         */

        currentWaitingThread = queue.poll();

        if(currentWaitingThread != null) {
          currentWaitingThread.condition.signal();

          // The thread that was waiting for permits may not be the highest priority one...
          if(currentWaitingThread.getPriority() > currentThread.getPriority()) {
            continue;  // ... it wasn't, back into the queue it goes!

            // Note: The higher priority thread that was awakened won't have to wait as the permits are already available and not consumed yet.
          }
        }

        // Current thread was the highest priority one, consume the permits, and allow thread to continue:
        availablePermits -= 1.0;
        break;
      }
      finally {
        queueLock.unlock();
      }
    }
  }

  // Only call while holding the queue lock:
  private void waitForSufficientPermits() throws InterruptedException {
    updatePermits();

    while(availablePermits < 1.0) {
      // Sleep roughly the amount of time it would take for a permit to become available
      queueWaitCondition.awaitNanos((long)((1.0 - availablePermits) / permitsPerNanoSecond));

      updatePermits();
    }
  }

  private void waitForNotification(ThreadWrapper currentThread) throws InterruptedException {
    while(currentWaitingThread != currentThread) {
      currentThread.condition.await();
    }
  }

  // Only call while holding the queue lock:
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
    final Condition condition;

    ThreadWrapper(Thread thread, Condition condition) {
      this.thread = thread;
      this.condition = condition;
    }

    int getPriority() {
      return this.thread.getPriority() - (this.thread.isVirtual() ? 10 : 0);
    }
  }
}

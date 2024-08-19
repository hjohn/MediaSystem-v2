package hs.mediasystem.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriorityRateLimiterTest {

  @Test
  public void shouldLimitAtFixedRate() throws InterruptedException {
    List<Long> timestamps = new ArrayList<>();

    timestamps.add(System.nanoTime());

    PriorityRateLimiter rateLimiter = new PriorityRateLimiter(1, 0.1);  // 1 per 0.1 s

    for(int i = 0; i < 10; i++) {
      rateLimiter.acquire();
      timestamps.add(System.nanoTime());
    }

    for(int i = 0; i < 10; i++) {
      long diff = (timestamps.get(i + 1) - timestamps.get(i)) / 1000L / 1000L;

      assertTrue(Math.abs(diff - 100) < 10, "Expected a time close to 100 ms, but got: " + diff + " ms");
    }
  }

  @Test
  public void shouldBurstThenLimitAtFixedRate() throws InterruptedException {
    PriorityRateLimiter rateLimiter = new PriorityRateLimiter(5, 0.5);  // Burst of 5 permits, then one per 100 ms

    Thread.sleep(600);  // Allow 5 permits to build up, takes 5 * 100 ms

    List<Long> timestamps = new ArrayList<>();
    timestamps.add(System.nanoTime());

    for(int i = 0; i < 20; i++) {
      rateLimiter.acquire();
      timestamps.add(System.nanoTime());
    }

    for(int i = 0; i < 5; i++) {
      long diff = (timestamps.get(i + 1) - timestamps.get(i)) / 1000L / 1000L;

      assertTrue(Math.abs(diff) < 10, "Expected a time close to 0 ms, but got: " + diff + " ms");
    }

    for(int i = 5; i < 20; i++) {
      long diff = (timestamps.get(i + 1) - timestamps.get(i)) / 1000L / 1000L;

      assertTrue(Math.abs(diff - 100) < 10, "Expected a time close to 100 ms, but got: " + diff + " ms");
    }
  }

  @Test
  public void higherPriorityThreadShouldGetPermitsFirst() throws InterruptedException {
    PriorityRateLimiter rateLimiter = new PriorityRateLimiter(1, 0.02);  // 1 per 20 ms
    AtomicLong time1 = new AtomicLong(0);
    AtomicLong time2 = new AtomicLong(0);

    Thread thread1 = createThread(Thread.NORM_PRIORITY - 1, () -> {
      long startTime = System.nanoTime();

      for(int i = 0; i < 10; i++) {
        try {
          rateLimiter.acquire();
        }
        catch(InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }

      time1.set(System.nanoTime() - startTime);
    });

    thread1.start();

    Thread.sleep(35);

    Thread thread2 = createThread(Thread.NORM_PRIORITY, () -> {
      long startTime = System.nanoTime();

      for(int i = 0; i < 10; i++) {
        try {
          rateLimiter.acquire();
        }
        catch(InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }

      time2.set(System.nanoTime() - startTime);
    });

    thread2.start();

    thread1.join();
    thread2.join();

    assertTrue(time1.get() > time2.get() * 1.9);  // The total time taken by thread1 should be almost half the time of thread2
  }

  @Test
  public void stressTest() throws InterruptedException {
    PriorityRateLimiter rateLimiter = new PriorityRateLimiter(15, 0.0015);  // 15 per 15 ms
    List<Thread> threads = new ArrayList<>();

    for(int t = 0; t < 200; t++) {
      Thread thread = createThread(Thread.NORM_PRIORITY - (t % 3), () -> {
        for(int i = 0; i < 50; i++) {
          try {
            rateLimiter.acquire();
          }
          catch(InterruptedException e) {
            throw new IllegalStateException(e);
          }
        }
      });

      thread.start();
      threads.add(thread);
    }

    for(Thread t : threads) {
      t.join();
    }
  }

  public Thread createThread(int priority, Runnable runnable) {
    Thread thread = new Thread(runnable);

    thread.setPriority(priority);

    return thread;
  }
}

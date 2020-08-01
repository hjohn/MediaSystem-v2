package hs.mediasystem.util;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterTest {

  @Test
  public void shouldLimitAtFixedRate() {
    RateLimiter rateLimiter = new RateLimiter(5, 0.1);
    List<Long> timestamps = new ArrayList<>();
    timestamps.add(System.nanoTime());

    for(int i = 0; i < 10; i++) {
      rateLimiter.acquire();
      timestamps.add(System.nanoTime());
    }

    for(int i = 0; i < 10; i++) {
      assertTrue(timestamps.get(i + 1) - timestamps.get(i) < (20L + 4L) * 1000L * 1000L);
      assertTrue(timestamps.get(i + 1) - timestamps.get(i) > (20L - 4L) * 1000L * 1000L);
    }
  }

  @Test
  public void shouldBurstThenLimitAtFixedRate() throws InterruptedException {
    RateLimiter rateLimiter = new RateLimiter(5, 0.1);

    Thread.sleep(300);  // Allow burst to build up

    List<Long> timestamps = new ArrayList<>();
    timestamps.add(System.nanoTime());

    for(int i = 0; i < 20; i++) {
      rateLimiter.acquire();
      timestamps.add(System.nanoTime());
    }

    for(int i = 0; i < 5; i++) {
      assertTrue(timestamps.get(i + 1) - timestamps.get(i) < 4L * 1000L * 1000L);
    }

    for(int i = 5; i < 20; i++) {
      assertTrue(timestamps.get(i + 1) - timestamps.get(i) < (20L + 4L) * 1000L * 1000L);
      assertTrue(timestamps.get(i + 1) - timestamps.get(i) > (20L - 4L) * 1000L * 1000L);
    }
  }
}

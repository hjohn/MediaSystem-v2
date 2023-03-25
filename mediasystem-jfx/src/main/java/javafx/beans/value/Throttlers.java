package javafx.beans.value;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongBinaryOperator;

/**
 * Provides standard {@link Throttler}s which use an unspecified thread to
 * perform property updates.
 */
public class Throttlers {
  protected static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(0);

  public static Throttler debounce(Duration quietPeriod) {
    return new IndependentThrottler(Throttler.IntervalHandler.debounce(quietPeriod));
  }

  public static Throttler debounceTrailing(Duration quietPeriod) {
    return new IndependentThrottler(Throttler.IntervalHandler.debounceTrailing(quietPeriod));
  }

  public static Throttler throttle(Duration period) {
    return new IndependentThrottler(Throttler.IntervalHandler.debounce(period));
  }

  public static Throttler throttleTrailing(Duration period) {
    return new IndependentThrottler(Throttler.IntervalHandler.debounce(period));
  }

  private static class IndependentThrottler implements Throttler {
    private final LongBinaryOperator intervalHandler;

    IndependentThrottler(LongBinaryOperator intervalHandler) {
      this.intervalHandler = intervalHandler;
    }

    @Override
    public Future<?> schedule(Runnable command, long nanos) {
      return SCHEDULER.schedule(command, nanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public long nanoTime() {
      return System.nanoTime();
    }

    @Override
    public void update(Runnable runnable) {
      runnable.run();
    }

    @Override
    public long determineInterval(long elapsed, long elapsedSinceLastChange) {
      return intervalHandler.applyAsLong(elapsed, elapsedSinceLastChange);
    }
  }
}

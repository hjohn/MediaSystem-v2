package javafx.throttle;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.LongBinaryOperator;

import javafx.application.Platform;
import javafx.beans.value.Throttler;
import javafx.beans.value.Throttlers;

/**
 * Provides standard {@link Throttler}s which integrate with JavaFX by performing
 * property updates using {@link Platform#runLater(Runnable)}.
 */
public class FXThrottlers extends Throttlers {
  public static Throttler debounce(Duration quietPeriod) {
    return new FXThrottler(Throttler.IntervalHandler.debounce(quietPeriod));
  }

  public static Throttler debounceTrailing(Duration quietPeriod) {
    return new FXThrottler(Throttler.IntervalHandler.debounceTrailing(quietPeriod));
  }

  public static Throttler throttle(Duration period) {
    return new FXThrottler(Throttler.IntervalHandler.debounce(period));
  }

  public static Throttler throttleTrailing(Duration period) {
    return new FXThrottler(Throttler.IntervalHandler.debounce(period));
  }

  private static class FXThrottler implements Throttler {
    private final LongBinaryOperator intervalHandler;

    FXThrottler(LongBinaryOperator intervalHandler) {
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
      Platform.runLater(runnable);
    }

    @Override
    public long determineInterval(long elapsed, long elapsedSinceLastChange) {
      return intervalHandler.applyAsLong(elapsed, elapsedSinceLastChange);
    }
  }
}

package javafx.beans.value;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.function.LongBinaryOperator;

public interface Throttler {

    /**
     * Schedules a command to run on an unspecified thread after the time
     * given by {@code nanos} elapses.
     *
     * @param command a command to run, cannot be {@code null}
     * @param nanos a time in nanoseconds
     * @return a {@link Future} to allow canceling the command, never {@code null}
     */
    Future<?> schedule(Runnable command, long nanos);

    /**
     * Provides the current time in nanoseconds.
     *
     * @return the current time in nanoseconds
     */
    long nanoTime();

    /**
     * Runs the given command as soon as possible on a thread specified by this
     * throttler for updating property values.
     *
     * @param command a command to run, cannot be {@code null}
     */
    void update(Runnable command);

    /**
     * Given the current elapsed time in the current change window, and the
     * amount of time elapsed since the last change was detected, determines
     * if and by how much the current change window should be extended.
     *
     * @param elapsed nanoseconds elapsed since the start of the current change window
     * @param elapsedSinceLastChange nanoseconds elapsed since the last change
     * @return nanoseconds to extend the window with
     */
    long determineInterval(long elapsed, long elapsedSinceLastChange);


    class IntervalHandler {
        public static final long END_INTERVAL_AND_EMIT = encode(0, true);

        public static LongBinaryOperator debounce(Duration quietPeriod) {
            long nanos = toNanos(quietPeriod);

            return (elapsed, elapsedSinceLastChange) ->
                elapsedSinceLastChange < nanos ? encode(nanos - elapsedSinceLastChange, elapsed == 0) : END_INTERVAL_AND_EMIT;
        }

        public static LongBinaryOperator debounceTrailing(Duration quietPeriod) {
            long nanos = toNanos(quietPeriod);

            return (elapsed, elapsedSinceLastChange) ->
                elapsedSinceLastChange < nanos ? encode(nanos - elapsedSinceLastChange, false) : END_INTERVAL_AND_EMIT;
        }

        public static LongBinaryOperator throttle(Duration period) {
            long nanos = toNanos(period);

            return (elapsed, elapsedSinceLastChange) ->
                elapsedSinceLastChange < nanos ? encode(nanos, true) : END_INTERVAL_AND_EMIT;
        }

        public static LongBinaryOperator throttleTrailing(Duration period) {
            long nanos = toNanos(period);

            return (elapsed, elapsedSinceLastChange) ->
                elapsedSinceLastChange < nanos ? encode(nanos, elapsedSinceLastChange != 0) : END_INTERVAL_AND_EMIT;
        }

        private static long toNanos(Duration quietPeriod) {
            long nanos = quietPeriod.toNanos();

            if (nanos <= 0) {
                throw new IllegalArgumentException("quietPeriod must be a positive duration: " + quietPeriod);
            }

            return nanos;
        }

        private static long encode(long nanos, boolean emit) {
            if(nanos < 0) {
                throw new IllegalArgumentException("nanos cannot be negative: " + nanos);
            }

            return emit ? nanos : ~nanos;
        }
    }
}

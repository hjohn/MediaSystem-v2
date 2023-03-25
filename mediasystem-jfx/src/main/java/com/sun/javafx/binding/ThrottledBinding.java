package com.sun.javafx.binding;

import java.util.Objects;
import java.util.concurrent.Future;

import javafx.beans.value.ObservableValue;
import javafx.beans.value.Throttler;

/**
 * A binding that can delay or skip values from a given source controlled
 * by a {@link Throttler}.<p>
 *
 * The binding only throttles when it is observed. When unobserved, the
 * bindings mirrors its source.
 *
 * @param <T> the type of the wrapped {@code Object}
 */
public class ThrottledBinding<T> extends LazyObjectBinding<T> {
    private final ObservableValue<T> source;
    private final Throttler throttler;

    // The following fields are protected by "lock" and should only be accessed when it is held:
    private T cachedValue;
    private long lastSourceChangeNanos;
    private Future<?> task;  // a currently running task
    private long elapsedTimeStart;  // start time of timing window, resets on emission
    private boolean subscribed;  // similar to #isObserved, but with slightly different timing to allow for updating the cached value at the correct moment

    /**
     * Creates a new throttled binding for the given source, its behavior controlled
     * by the given {@link Throttler}.
     *
     * @param source a source observable, cannot be {@code null}
     * @param throttler a {@link Throttler} that determines the behavior of this binding,
     *   cannot be {@code null}
     * @throws ArithmeticException when delay exceeds {@link Long#MAX_VALUE} nanoseconds
     * @throws NullPointerException when a parameter is {@code null}
     * @throws IllegalArgumentException when delay is not a positive duration
     */
    public ThrottledBinding(ObservableValue<T> source, Throttler throttler) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.throttler = Objects.requireNonNull(throttler, "throttler cannot be null");
    }

    @Override
    protected Subscription observeSources() {
        synchronized(this) {
            this.cachedValue = source.getValue();
            this.subscribed = true;

            return Subscription.subscribe(source, this::sourceValueChanged)
                .and(this::onUnsubscribe);
        }
    }

    @Override
    protected T computeValue() {
        synchronized(this) {
            if (subscribed) {
                return cachedValue;
            }
        }

        return source.getValue();
    }

    private void sourceValueChanged(T value) {
        synchronized(this) {
            long now = throttler.nanoTime();

            lastSourceChangeNanos = now;

            if (task == null && isValid() && value != getValue()) {
                elapsedTimeStart = now;

                updateAndReschedule(now);
            }
        }
    }

    private void onUnsubscribe() {
        synchronized(this) {
            subscribed = false;
            cachedValue = null;  // avoid referencing outdated values

            if(task != null) {
              task.cancel(false);
              task = null;
            }
        }
    }

    /**
     * Called by the scheduler when a timer elapses. When the timer elapsed, the cached value
     * can be updated and/or the timer is rescheduled. Only one timer ever runs simultaneously
     * for an instance.<p>
     *
     * Note: as this method is not called on the FX Thread, it should not directly or indirectly
     * call methods like invalidate or getValue.
     */
    private void timerElapsed() {
        synchronized(this) {

            /*
             * Check if the timer is supposed to be running first, as it is possible
             * the timer was running still when it shouldn't anymore (this can happen when
             * this binding was previously observed, but has become unobserved).
             */

            if (task != null) {
                task = null;

                updateAndReschedule(throttler.nanoTime());
            }
        }
    }

    /**
     * Updates the cached value and reschedules the timer when needed. This
     * should only be called when the lock is held.
     *
     * @param now the current time in nanoseconds
     */
    private void updateAndReschedule(long now) {
        long encodedInterval = throttler.determineInterval(now - elapsedTimeStart, now - lastSourceChangeNanos);
        long interval = encodedInterval < 0 ? ~encodedInterval : encodedInterval;
        boolean emit = encodedInterval >= 0;

        startTimer(interval);

        if (emit) {
            elapsedTimeStart = now;

            updateCachedValue();
        }
    }

    /**
     * Starts the timer if needed. This should only be called when the lock is
     * held.
     *
     * @param nanos the number of nanoseconds after which the timer should trigger
     */
    private void startTimer(long nanos) {
        if (nanos > 0) {
            task = throttler.schedule(this::timerElapsed, nanos);
        }
    }

    private void updateCachedValue() {
        // Perform property change and invalidation on a provided thread:
        throttler.update(() -> {
            T value = source.getValue();

            synchronized(this) {
                if(cachedValue != value) {
                    cachedValue = value;
                    invalidate();
                }
            }
        });
    }
}

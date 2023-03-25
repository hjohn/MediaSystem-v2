package com.sun.javafx.binding;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongBinaryOperator;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.Throttler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ThrottledBindingTest {
  private final StringProperty source = new SimpleStringProperty();
  private final AtomicLong nanos = new AtomicLong();

  private ScheduledCommand activeCommand;

  record ScheduledCommand(FutureTask<?> task, long deadline) {}

  @Test
  void testDebounceTrailingWithMaximum() {
    Supplier<ObservableValue<String>> bindingSupplier = () -> new ThrottledBinding<>(source, createThrottler(
      (elapsed, lastChange) ->
        elapsed >= 9 ? 4 :
          lastChange < 4 ? ~Math.min(4 - lastChange, 9 - elapsed) : 0
    ));

    assertUnobserved(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------",
      "0-a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------"
    );

    assertChanges(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------",
      "0----------c---d-----e---------g-------------------h----------e--------i--------n--------r--------w--------z------"
    );

    assertInvalidations(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------",
      "-----------i---i-----i---------i-------------?-----i----------i--------i--------i--------i--------i--------i------"
    );
  }

  @Test
  void testDebounceTrailing() {
    Supplier<ObservableValue<String>> bindingSupplier = () ->
      new ThrottledBinding<>(source, createThrottler(Throttler.IntervalHandler.debounceTrailing(Duration.ofNanos(4))));

    assertUnobserved(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------",
      "0-a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------"
    );

    assertChanges(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------",
      "0----------c---d-----e---------g-------------------h-------------------------------------------------------z------"
    );

    assertInvalidations(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z----------",
      "-----------i---i-----i---------i-------------?-----i-------------------------------------------------------i------"
    );
  }

  @Test
  void testThrottleTrailing() {
    Supplier<ObservableValue<String>> bindingSupplier = () ->
      new ThrottledBinding<>(source, createThrottler(Throttler.IntervalHandler.throttleTrailing(Duration.ofNanos(4))));

    assertUnobserved(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----",
      "0-a-b--c---d-----e-------f-g-----------f-g-----"
    );

    assertChanges(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----",
      "0-----b---c---d---e----------g-----------------"
    );

    assertInvalidations(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----",
      "------i---i---i---i----------i-------------?---"
    );
  }

  @Test
  void testDebounceLeadingAndTrailing() {
    Supplier<ObservableValue<String>> bindingSupplier = () ->
      new ThrottledBinding<>(source, createThrottler(Throttler.IntervalHandler.debounce(Duration.ofNanos(4))));

    assertUnobserved(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----",
      "0-a-b--c---d-----e-------f-g-----------f-g-----h-----"
    );

    assertChanges(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----",
      "0-a--------d-----e-------f-----g-------f-----g-h-----"
    );

    assertInvalidations(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----h-----",
      "--i--------i-----i-------i-----i-------i-----i-i-----"
    );
  }

  @Test
  void testThrottleLeadingAndTrailing() {
    Supplier<ObservableValue<String>> bindingSupplier = () ->
      new ThrottledBinding<>(source, createThrottler(Throttler.IntervalHandler.throttle(Duration.ofNanos(4))));

    assertUnobserved(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----",
      "0-a-b--c---d-----e-------f-g-----------f-g-----"
    );

    assertChanges(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----",
      "0-a---b---c---d---e------f---g---------f---g---"
    );

    assertInvalidations(
      bindingSupplier,
      "--a-b--c---d-----e-------f-g-----------f-g-----",
      "--i---i---i---i---i------i---i---------i---i---"
    );
  }

  private void assertUnobserved(Supplier<ObservableValue<String>> observableValueSupplier, String pattern, String expectation) {
    if(pattern == null || expectation == null) {
      throw new IllegalArgumentException();
    }
    if(pattern.length() != expectation.length()) {
      throw new IllegalArgumentException();
    }
    if(expectation.startsWith("-")) {
      throw new IllegalArgumentException();
    }

    source.set("0");

    ObservableValue<String> observableValue = observableValueSupplier.get();
    int patternLength = pattern.length();
    char[] src = new char[patternLength];
    char[] dst = new char[patternLength];

    for(int i = 0; i < patternLength; i++) {
      src[i] = pattern.charAt(i);
      dst[i] = expectation.charAt(i) == '-' ? dst[i - 1] : expectation.charAt(i);
    }

    for(int i = 0; i < patternLength; i++) {
      char s = src[i];
      char d = dst[i];

      if(s != '-') {
        source.setValue("" + s);
      }

      String value = observableValue.getValue();

      assertEquals("" + d, value, "\n" + pattern + "\n" + expectation + "\n" + (expectation.subSequence(0, i)) + "^\n");

      tick(1L);
    }
  }

  private void assertChanges(Supplier<ObservableValue<String>> observableValueSupplier, String pattern, String expectation) {
    if(pattern == null || expectation == null) {
      throw new IllegalArgumentException();
    }
    if(pattern.length() != expectation.length()) {
      throw new IllegalArgumentException();
    }
    if(expectation.startsWith("-")) {
      throw new IllegalArgumentException();
    }

    source.set("0");

    ObservableValue<String> observableValue = observableValueSupplier.get();
    AtomicReference<String> change = new AtomicReference<>();
    ChangeListener<String> listener = (obs, old, current) -> change.set(current);

    observableValue.addListener(listener);

    int patternLength = pattern.length();
    char[] src = new char[patternLength];
    char[] dst = new char[patternLength];

    for(int i = 0; i < patternLength; i++) {
      src[i] = pattern.charAt(i);
      dst[i] = expectation.charAt(i) == '-' ? dst[i - 1] : expectation.charAt(i);
    }

    for(int i = 0; i < patternLength; i++) {
      char s = src[i];
      char d = dst[i];

      if(s != '-') {
        source.setValue("" + s);
      }

      String value = observableValue.getValue();

      assertEquals("" + d, value, "\n" + pattern + "\n" + expectation + "\n" + (expectation.subSequence(0, i)) + "^\n");

      if(value.equals("0")) {
        assertNull(change.get());
      }
      else {
        assertEquals(value, change.get());
      }

      tick(1L);
    }

    observableValue.removeListener(listener);
  }

  /*
   * Asserts invalidation patterns.
   *
   * - = no invalidation expected
   * i = invalidation expected
   * ? = invalidation allowed but not required
   */
  private void assertInvalidations(Supplier<ObservableValue<String>> observableValueSupplier, String pattern, String expectation) {
    if(pattern == null || expectation == null) {
      throw new IllegalArgumentException();
    }
    if(pattern.length() != expectation.length()) {
      throw new IllegalArgumentException();
    }
    if(!expectation.matches("[-i\\?]*")) {
      throw new IllegalArgumentException();
    }

    source.set("0");

    ObservableValue<String> observableValue = observableValueSupplier.get();
    AtomicBoolean invalidated = new AtomicBoolean();
    InvalidationListener listener = obs -> invalidated.set(true);

    observableValue.addListener(listener);

    int patternLength = pattern.length();
    char[] src = new char[patternLength];
    char[] dst = new char[patternLength];

    for(int i = 0; i < patternLength; i++) {
      src[i] = pattern.charAt(i);
      dst[i] = expectation.charAt(i);
    }

    for(int i = 0; i < patternLength; i++) {
      char s = src[i];
      char d = dst[i];

      observableValue.getValue();  // make it valid before changing source

      if(s != '-') {
        source.setValue("" + s);
      }

      boolean wasInvalidated = invalidated.getAndSet(false);

      if(d != '?') {  // skip if invalidation is allowed, but not required
        assertEquals(d == 'i', wasInvalidated, "\n" + pattern + "\n" + expectation + "\n" + (expectation.subSequence(0, i)) + "^\n");
      }

      tick(1L);
    }

    observableValue.removeListener(listener);
  }

  private void tick(long n) {
    long time = nanos.addAndGet(n);
    ScheduledCommand command = activeCommand;

    if(command != null && command.deadline <= time) {
      activeCommand = null;
      command.task.run();
    }
  }

  private Throttler createThrottler(LongBinaryOperator intervalHandler) {
    return new Throttler() {

      @Override
      public Future<?> schedule(Runnable command, long delay) {
        assertNull(activeCommand);

        FutureTask<?> futureTask = new FutureTask<>(command, null);

        activeCommand = new ScheduledCommand(futureTask, nanos.get() + delay);

        return futureTask;
      }

      @Override
      public long nanoTime() {
        return nanos.get();
      }

      @Override
      public void update(Runnable runnable) {
        runnable.run();
      }

      @Override
      public long determineInterval(long elapsed, long elapsedSinceLastChange) {
        return intervalHandler.applyAsLong(elapsed, elapsedSinceLastChange);
      }
    };
  }
}

package hs.mediasystem.util.events;

import hs.mediasystem.util.events.store.EventStore;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AbstractEventStoreTest {
  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

  private final EventStore<String> store;
  private final int firstIndex;

  protected AbstractEventStoreTest(EventStore<String> store, int firstIndex) {
    this.store = store;
    this.firstIndex = firstIndex;
  }

  @Nested
  class WhenEmpty extends Invariants {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1000})
    void pollFromNonNegativeIndexShouldReturnNull(int offset) {
      assertThat(store.poll(offset + firstIndex)).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1000})
    void takeFromNonNegativeIndexShouldBlockUntilInterrupted(int offset) {
      Thread.currentThread().interrupt();
      assertThatThrownBy(() -> store.take(offset + firstIndex)).isExactlyInstanceOf(InterruptedException.class);
    }

    @Test
    @Timeout(5)
    void takeShouldReturnWhenEventBecomesAvailable() throws InterruptedException {
      EXECUTOR.schedule(() -> store.append("A"), 50, TimeUnit.MILLISECONDS);

      assertThat(store.take(firstIndex).event()).isEqualTo("A");
    }

    @Nested
    class AndSomeEventsAreAdded extends Invariants {
      {
        store.append("A");
        store.append("B");
        store.append("C");
      }

      @ParameterizedTest
      @ValueSource(ints = {3, 5, 1000})
      void pollOfNonExistingIndexShouldReturnNull(int offset) {
        assertThat(store.poll(offset + firstIndex)).isNull();
      }

      @ParameterizedTest
      @ValueSource(ints = {3, 5, 1000})
      void takeOfNonExistingIndexShouldBlockUntilInterrupted(int offset) {
        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> store.take(offset + firstIndex)).isExactlyInstanceOf(InterruptedException.class);
      }

      @Test
      void pollShouldReturnEvent() {
        assertThat(store.poll(firstIndex).event()).isEqualTo("A");
        assertThat(store.poll(firstIndex + 1).event()).isEqualTo("B");
        assertThat(store.poll(firstIndex + 2).event()).isEqualTo("C");
      }

      @Test
      void takeShouldReturnEvent() throws InterruptedException {
        assertThat(store.take(firstIndex).event()).isEqualTo("A");
        assertThat(store.take(firstIndex + 1).event()).isEqualTo("B");
        assertThat(store.take(firstIndex + 2).event()).isEqualTo("C");
      }
    }
  }

  class Invariants {
    @Test
    void pollWithNegativeValueShouldBeRejected() {
      assertThatThrownBy(() -> store.poll(-1)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void takeWithNegativeValueShouldBeRejected() {
      assertThatThrownBy(() -> store.take(-1)).isExactlyInstanceOf(IllegalArgumentException.class);
    }
  }
}

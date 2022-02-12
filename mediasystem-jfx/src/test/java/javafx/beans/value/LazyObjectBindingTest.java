package javafx.beans.value;

import com.sun.javafx.binding.Subscription;

import javafx.beans.InvalidationListener;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LazyObjectBindingTest {
  private int startObservingCalls;
  private int computeValueCalls;
  private int stopObservingCalls;

  private LazyObjectBinding<String> binding = new LazyObjectBinding<>() {
    @Override
    protected String computeValue() {
      computeValueCalls++;

      return null;
    }

    @Override
    protected Subscription observeInputs() {
      startObservingCalls++;

      return () -> {
        stopObservingCalls++;
      };
    }
  };

  void resetCounters() {
    startObservingCalls = 0;
    computeValueCalls = 0;
    stopObservingCalls = 0;
  }

  @Test
  void shouldBeInvalidInitially() {
    assertFalse(binding.isValid());
  }

  @Nested
  class WhenObservedWithInvalidationListener {
    private InvalidationListener invalidationListener = obs -> {};

    {
      binding.addListener(invalidationListener);
    }

    @Test
    void shouldBeValid() {
      assertTrue(binding.isValid());
    }

    @Test
    void shouldStartObservingSource() {
      assertEquals(1, startObservingCalls);
    }

    @Test
    void shouldNotStopObservingSource() {
      assertEquals(0, stopObservingCalls);
    }

    @Test
    void shouldCallComputeValueOneOrTwoTimes() {

      /*
       * The binding is made valid twice currently, once when
       * the listener is registered, and again after the observing of
       * inputs starts. The first time the binding does not become
       * valid because it is not yet considered "observed" as the
       * computeValue call occurs in the middle of the listener
       * registration process.
       */

      assertTrue(computeValueCalls >= 1 && computeValueCalls <= 2);
    }

    @Nested
    class AndWhenObservedAgain {
      private ChangeListener<String> changeListener = (obs, old, current) -> {};

      {
        resetCounters();
        binding.addListener(changeListener);
      }

      @Test
      void shouldStillBeValid() {
        assertTrue(binding.isValid());
      }

      @Test
      void shouldNotStartObservingSourceAgain() {
        assertEquals(0, startObservingCalls);
      }

      @Test
      void shouldNotStopObservingSource() {
        assertEquals(0, stopObservingCalls);
      }

      @Test
      void shouldNotComputeValueAgain() {
        assertEquals(0, computeValueCalls);
      }

      @Nested
      class AndThenOneObserverIsRemoved {
        {
          resetCounters();
          binding.removeListener(changeListener);
        }

        @Test
        void shouldStillBeValid() {
          assertTrue(binding.isValid());
        }

        @Test
        void shouldNotStartObservingSourceAgain() {
          assertEquals(0, startObservingCalls);
        }

        @Test
        void shouldNotStopObservingSource() {
          assertEquals(0, stopObservingCalls);
        }

        @Test
        void shouldNotComputeValueAgain() {
          assertEquals(0, computeValueCalls);
        }

        @Nested
        class AndThenTheLastObserverIsRemoved {
          {
            resetCounters();
            binding.removeListener(invalidationListener);
          }

          @Test
          void shouldNotStartObservingSource() {
            assertEquals(0, startObservingCalls);
          }

          @Test
          void shouldStopObservingSource() {
            assertEquals(1, stopObservingCalls);
          }

          @Test
          void shouldNotComputeValue() {
            assertEquals(0, computeValueCalls);
          }

          @Test
          void shouldNoLongerBeValid() {
            assertFalse(binding.isValid());
          }

          @Nested
          class AndTheListenerIsRemovedAgain {
            {
              resetCounters();
              binding.removeListener(invalidationListener);
            }

            @Test
            void shouldNotStartObservingSource() {
              assertEquals(0, startObservingCalls);
            }

            @Test
            void shouldNotStopObservingSource() {
              assertEquals(0, stopObservingCalls);
            }

            @Test
            void shouldNotComputeValue() {
              assertEquals(0, computeValueCalls);
            }

            @Test
            void shouldNotBeValid() {
              assertFalse(binding.isValid());
            }
          }
        }
      }
    }
  }
}

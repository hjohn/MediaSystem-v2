package hs.mediasystem.util.javafx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedTimeTrackerTest {

  @Test
  void shouldTrackNestedTimes() {
    NestedTimeTracker tracker = new NestedTimeTracker();

    tracker.enterNested(0);

    // Here 22 ms is spent in level 1

    {
      tracker.enterNested(22);

      // Here 8 ms is spent in level 2 (between time 22 and 30)

      assertEquals(8, tracker.exitNested(30));
    }

    // Here 10 ms is spent in level 1 (between time 30 and 40)

    {
      tracker.enterNested(40);

      // Here 5 ms is spent in level 2 (between time 40 and 45)

      {
        tracker.enterNested(45);

        // Here 3 ms is spent in level 3 (between time 45 and 48)

        assertEquals(3, tracker.exitNested(48));
      }

      {
        tracker.enterNested(48);

        // Here 1 ms is spent in level 3 (between time 48 and 49)

        {
          tracker.enterNested(49);

          // Here 1 ms is spent in level 4 (between time 49 and 50)

          assertEquals(1, tracker.exitNested(50));
        }

        // Here 2 ms is spent in level 3 (between time 50 and 52)

        assertEquals(1 + 2, tracker.exitNested(52));  // assert total time taken in level 3
      }

      // Here 0 ms is spent in level 2 (between time 52 and 52)

      assertEquals(5, tracker.exitNested(52));
    }

    // Here 18 ms is spent in level 1 (between time 52 and 70)

    assertEquals(22 + 10 + 18, tracker.exitNested(70));
  }
}

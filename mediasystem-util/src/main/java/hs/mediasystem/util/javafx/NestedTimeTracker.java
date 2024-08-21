package hs.mediasystem.util.javafx;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks time spent (in nanoseconds) between calls of {@link #enterNested()} and {@link #exitNested()}
 * excluding time spent in deeper nested levels.  This allows to find the time spent at a certain
 * nesting level without including time spent at any deeper nesting.
 */
public class NestedTimeTracker {
  private final Deque<Long> startTimes = new ArrayDeque<>();
  private final Deque<Long> cumulativeDurations = new ArrayDeque<>();

  {
    cumulativeDurations.add(0L);  // Add root level, always present (this is never used, the entry is there to avoid having to check if we're at root level)
  }

  public int getCurrentLevel() {
    return startTimes.size();
  }

  public void enterNested() {
    enterNested(System.nanoTime());
  }

  public void enterNested(long nanos) {
    startTimes.addLast(nanos);
    cumulativeDurations.add(0L);
  }

  public void exitNested() {
    exitNested(System.nanoTime());
  }

  public long exitNested(long nanos) {
    long startTime = startTimes.removeLast();
    long timeSpentInNested = cumulativeDurations.removeLast();  // nested time, not part of this level
    long totalTime = nanos - startTime;  // total time, including nested levels
    long timeSpentAtThisLevel = totalTime - timeSpentInNested;

    // Consolidate total time spent in next higher level:
    cumulativeDurations.addLast(cumulativeDurations.removeLast() + totalTime);

    return timeSpentAtThisLevel;
  }
}

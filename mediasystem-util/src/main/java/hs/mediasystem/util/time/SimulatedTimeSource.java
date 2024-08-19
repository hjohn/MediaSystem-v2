package hs.mediasystem.util.time;

import java.time.Duration;
import java.time.Instant;

public class SimulatedTimeSource implements TimeSource {
  private Instant currentTime;

  public SimulatedTimeSource(Instant initialTime) {
    this.currentTime = initialTime;
  }

  @Override
  public synchronized void sleep(Duration duration) throws InterruptedException {
    Instant wakeUpTime = currentTime.plus(duration);

    while(wakeUpTime.isAfter(currentTime)) {
      wait();
    }
  }

  public synchronized void advanceTime(Duration duration) {
    currentTime = currentTime.plus(duration);
    // Notify all waiting threads
    notifyAll();
  }

  public synchronized void advanceTime(long milliseconds) {
    advanceTime(Duration.ofMillis(milliseconds));
  }

  @Override
  public Instant instant() {
    return currentTime;
  }
}

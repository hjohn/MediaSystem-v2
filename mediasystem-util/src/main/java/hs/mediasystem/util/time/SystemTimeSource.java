package hs.mediasystem.util.time;

import java.time.Duration;
import java.time.Instant;

class SystemTimeSource implements TimeSource {
  static final TimeSource INSTANCE = new SystemTimeSource();

  @Override
  public void sleep(Duration duration) throws InterruptedException {
    Thread.sleep(duration);
  }

  @Override
  public Instant instant() {
    return Instant.now();
  }
}

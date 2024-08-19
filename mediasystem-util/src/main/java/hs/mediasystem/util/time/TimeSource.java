package hs.mediasystem.util.time;

import java.time.Duration;
import java.time.InstantSource;

public interface TimeSource extends InstantSource {

  static TimeSource system() {
    return SystemTimeSource.INSTANCE;
  }

  void sleep(Duration duration) throws InterruptedException;
}

package hs.mediasystem.db;

import java.time.Instant;

public class CachedStream {
  private final IdentifiedStream stream;
  private final int scannerId;
  private final Instant lastEnrichTime;
  private final Instant nextEnrichTime;

  public CachedStream(IdentifiedStream stream, int scannerId, Instant lastEnrichTime, Instant nextEnrichTime) {
    this.stream = stream;
    this.scannerId = scannerId;
    this.lastEnrichTime = lastEnrichTime;
    this.nextEnrichTime = nextEnrichTime;
  }

  public IdentifiedStream getIdentifiedStream() {
    return stream;
  }

  public int getScannerId() {
    return scannerId;
  }

  public Instant getLastEnrichTime() {
    return lastEnrichTime;
  }

  public Instant getNextEnrichTime() {
    return nextEnrichTime;
  }
}

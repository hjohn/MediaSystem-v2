package hs.mediasystem.db;

import java.time.Instant;

public class CachedStream {
  private final IdentifiedStream stream;
  private final int importSourceId;
  private final Instant creationTime;
  private final Instant lastEnrichTime;
  private final Instant nextEnrichTime;

  public CachedStream(IdentifiedStream stream, int importSourceId, Instant creationTime, Instant lastEnrichTime, Instant nextEnrichTime) {
    this.stream = stream;
    this.importSourceId = importSourceId;
    this.creationTime = creationTime;
    this.lastEnrichTime = lastEnrichTime;
    this.nextEnrichTime = nextEnrichTime;
  }

  public IdentifiedStream getIdentifiedStream() {
    return stream;
  }

  public int getImportSourceId() {
    return importSourceId;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public Instant getLastEnrichTime() {
    return lastEnrichTime;
  }

  public Instant getNextEnrichTime() {
    return nextEnrichTime;
  }
}

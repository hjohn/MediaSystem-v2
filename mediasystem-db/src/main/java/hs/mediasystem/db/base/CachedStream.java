package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.time.Instant;
import java.util.Optional;

public class CachedStream {
  private final Streamable streamable;
  private final Optional<Identification> identification;
  private final Instant creationTime;
  private final Instant lastEnrichTime;
  private final Instant nextEnrichTime;

  public CachedStream(Streamable streamable, Identification identification, Instant creationTime, Instant lastEnrichTime, Instant nextEnrichTime) {
    if(streamable == null) {
      throw new IllegalArgumentException("streamable cannot be null");
    }
    if(creationTime == null) {
      throw new IllegalArgumentException("creationTime cannot be null");
    }

    this.streamable = streamable;
    this.identification = Optional.ofNullable(identification);
    this.creationTime = creationTime;
    this.lastEnrichTime = lastEnrichTime;
    this.nextEnrichTime = nextEnrichTime;
  }

  public Streamable getStreamable() {
    return streamable;
  }

  public Optional<Identification> getIdentification() {
    return identification;
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

  public int getImportSourceId() {
    return streamable.getId().getImportSourceId();
  }
}

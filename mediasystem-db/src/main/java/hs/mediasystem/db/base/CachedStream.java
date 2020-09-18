package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

import java.time.Instant;
import java.util.Optional;

public class CachedStream {
  private final Streamable streamable;
  private final Optional<Identification> identification;
  private final Instant discoveryTime;
  private final Instant lastEnrichTime;
  private final Instant nextEnrichTime;

  public CachedStream(Streamable streamable, Identification identification, Instant discoveryTime, Instant lastEnrichTime, Instant nextEnrichTime) {
    if(streamable == null) {
      throw new IllegalArgumentException("streamable cannot be null");
    }
    if(discoveryTime == null) {
      throw new IllegalArgumentException("discoveryTime cannot be null");
    }

    this.streamable = streamable;
    this.identification = Optional.ofNullable(identification);
    this.discoveryTime = discoveryTime;
    this.lastEnrichTime = lastEnrichTime;
    this.nextEnrichTime = nextEnrichTime;
  }

  public Streamable getStreamable() {
    return streamable;
  }

  public Optional<Identification> getIdentification() {
    return identification;
  }

  public Instant getDiscoveryTime() {
    return discoveryTime;
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

package hs.mediasystem.db;

import hs.mediasystem.ext.basicmediatypes.MediaDescriptor;

import java.time.Instant;

public class CachedDescriptor {
  private final Instant lastUsedTime;
  private final MediaDescriptor descriptor;

  public CachedDescriptor(Instant lastUsedTime, MediaDescriptor descriptor) {
    if(lastUsedTime == null) {
      throw new IllegalArgumentException("lastUsedTime cannot be null");
    }
    if(descriptor == null) {
      throw new IllegalArgumentException("descriptor cannot be null");
    }

    this.lastUsedTime = lastUsedTime;
    this.descriptor = descriptor;
  }

  public Instant getLastUsedTime() {
    return lastUsedTime;
  }

  public MediaDescriptor getDescriptor() {
    return descriptor;
  }
}

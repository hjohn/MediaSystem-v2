package hs.mediasystem.db.base;

import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;

import java.time.Instant;

public class CachedDescriptor {
  private final Instant lastUsedTime;
  private final WorkDescriptor descriptor;

  public CachedDescriptor(Instant lastUsedTime, WorkDescriptor descriptor) {
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

  public WorkDescriptor getDescriptor() {
    return descriptor;
  }
}

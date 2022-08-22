package hs.mediasystem.mediamanager;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.WorkDescriptor;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;

public class MediaIdentification {
  private final Streamable streamable;
  private final Identification identification;
  private final WorkDescriptor descriptor;

  public MediaIdentification(Streamable streamable, Identification identification, WorkDescriptor descriptor) {
    if(streamable == null) {
      throw new IllegalArgumentException("streamable cannot be null");
    }
    if(identification == null) {
      throw new IllegalArgumentException("identification cannot be null");
    }
    if(descriptor == null && streamable.getParentId() == null) {
      throw new IllegalArgumentException("descriptor cannot be null for streamables without a parent");
    }

    this.streamable = streamable;
    this.identification = identification;
    this.descriptor = descriptor;
  }

  public Identification getIdentification() {
    return identification;
  }

  public WorkDescriptor getDescriptor() {
    return descriptor;
  }

  public Streamable getStreamable() {
    return streamable;
  }
}

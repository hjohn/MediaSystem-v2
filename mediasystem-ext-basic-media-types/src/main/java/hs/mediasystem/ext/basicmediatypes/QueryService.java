package hs.mediasystem.ext.basicmediatypes;

import java.util.Collections;
import java.util.Set;

public interface QueryService<D extends MediaDescriptor> {
  DataSource getDataSource();
  Result<D> query(Identifier identifier);

  public static class Result<D> {
    public static <D> Result<D> of(D mediaDescriptor, Set<Identification> newIdentifications) {
      return new Result<>(mediaDescriptor, newIdentifications);
    }

    public static <D> Result<D> of(D mediaDescriptor) {
      return new Result<>(mediaDescriptor, Collections.emptySet());
    }

    private final D mediaDescriptor;
    private final Set<Identification> newIdentifications;

    private Result(D mediaDescriptor, Set<Identification> newIdentifications) {
      if(mediaDescriptor == null) {
        throw new IllegalArgumentException("mediaDescriptor cannot be null");
      }
      if(newIdentifications == null) {
        throw new IllegalArgumentException("newIdentifications cannot be null");
      }

      this.mediaDescriptor = mediaDescriptor;
      this.newIdentifications = newIdentifications;
    }

    public D getMediaDescriptor() {
      return mediaDescriptor;
    }

    public Set<Identification> getNewIdentifications() {
      return newIdentifications;
    }
  }
}

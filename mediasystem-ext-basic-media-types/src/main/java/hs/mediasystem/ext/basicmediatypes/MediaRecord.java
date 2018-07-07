package hs.mediasystem.ext.basicmediatypes;

import java.util.Objects;

public class MediaRecord<D extends MediaDescriptor> {
  private final Identification identification;
  private final D mediaDescriptor;

  public MediaRecord(Identification identification, D mediaDescriptor) {
    if(identification == null) {
      throw new IllegalArgumentException("identification cannot be null");
    }

    this.identification = identification;
    this.mediaDescriptor = mediaDescriptor;
  }

  public Identification getIdentification() {
    return identification;
  }

  public D getMediaDescriptor() {
    return mediaDescriptor;
  }

  public DataSource getDataSource() {
    return identification.getIdentifier().getDataSource();
  }

  @Override
  public int hashCode() {
    return Objects.hash(identification, mediaDescriptor);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    MediaRecord<?> other = (MediaRecord<?>)obj;

    if(!identification.equals(other.identification)) {
      return false;
    }
    if(!Objects.equals(mediaDescriptor, other.mediaDescriptor)) {
      return false;
    }

    return true;
  }
}

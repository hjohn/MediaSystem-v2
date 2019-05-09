package hs.mediasystem.ext.basicmediatypes;

import java.util.Objects;

public class MediaRecord {
  private final Identifier identifier;
  private final Identification identification;
  private final MediaDescriptor mediaDescriptor;

  public MediaRecord(Identifier identifier, Identification identification, MediaDescriptor mediaDescriptor) {
    if(identifier == null) {
      throw new IllegalArgumentException("identifier cannot be null");
    }
    if(identification == null) {
      throw new IllegalArgumentException("identification cannot be null");
    }

    this.identifier = identifier;
    this.identification = identification;
    this.mediaDescriptor = mediaDescriptor;
  }

  public Identifier getIdentifier() {
    return identifier;
  }

  public Identification getIdentification() {
    return identification;
  }

  public MediaDescriptor getMediaDescriptor() {
    return mediaDescriptor;
  }

  public DataSource getDataSource() {
    return identifier.getDataSource();
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, identification, mediaDescriptor);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    MediaRecord other = (MediaRecord)obj;

    if(!identifier.equals(other.identifier)) {
      return false;
    }
    if(!identification.equals(other.identification)) {
      return false;
    }
    if(!Objects.equals(mediaDescriptor, other.mediaDescriptor)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "MediaRecord[" + identifier + " (" + identification + ") -> " + mediaDescriptor + "]";
  }
}

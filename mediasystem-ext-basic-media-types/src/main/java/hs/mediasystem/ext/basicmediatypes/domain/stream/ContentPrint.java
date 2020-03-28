package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.ContentID;

import java.util.Arrays;
import java.util.Objects;

public class ContentPrint {
  private final ContentID id;                  // an identifier that never changes for this particular ContentPrint
  private final Long size;
  private final byte[] hash;
  private final long lastModificationTime;

  public ContentPrint(ContentID id, Long size, long lastModificationTime, byte[] hash) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(hash == null) {
      throw new IllegalArgumentException("hash cannot be null");
    }

    this.id = id;
    this.size = size;
    this.lastModificationTime = lastModificationTime;
    this.hash = hash;
  }

  public ContentID getId() {
    return id;
  }

  public byte[] getHash() {
    return hash;
  }

  public Long getSize() {
    return size;
  }

  public long getLastModificationTime() {
    return lastModificationTime;
  }

  @Override
  public String toString() {
    return "ContentPrint(" + id + ",size=" + size + ",modTime=" + lastModificationTime + ",hash=" + Arrays.toString(hash) + ")";
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, size, Arrays.hashCode(hash), lastModificationTime);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ContentPrint other = (ContentPrint)obj;

    if(!Objects.equals(id, other.id)) {
      return false;
    }

    if(!Arrays.equals(hash, other.hash)) {
      return false;
    }

    if(lastModificationTime != other.lastModificationTime) {
      return false;
    }

    if(size == null) {
      if(other.size != null) {
        return false;
      }
    }
    else if(!size.equals(other.size)) {
      return false;
    }

    return true;
  }
}

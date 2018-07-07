package hs.mediasystem.db;

import hs.database.annotations.Embeddable;
import hs.database.annotations.EmbeddableColumn;

import java.util.Arrays;
import java.util.Objects;

@Embeddable
public class StreamStateRecordId {

  @EmbeddableColumn(1)
  private byte[] hash;

  @EmbeddableColumn(2)
  private Long size;

  @EmbeddableColumn(3)
  private long modTime;

  public StreamStateRecordId(byte[] hash, Long size, long modTime) {
    this.hash = hash;
    this.size = size;
    this.modTime = modTime;
  }

  public byte[] getHash() {
    return hash;
  }

  public void setHash(byte[] hash) {
    this.hash = hash;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public long getModTime() {
    return modTime;
  }

  public void setModTime(long modTime) {
    this.modTime = modTime;
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(hash), size, modTime);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    StreamStateRecordId other = (StreamStateRecordId)obj;

    if(!Arrays.equals(hash, other.hash)) {
      return false;
    }
    if(modTime != other.modTime) {
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

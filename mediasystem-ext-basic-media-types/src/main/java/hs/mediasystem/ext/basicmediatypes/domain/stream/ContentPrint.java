package hs.mediasystem.ext.basicmediatypes.domain.stream;

import hs.mediasystem.domain.stream.ContentID;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * A content print is a finger print style identifier.
 *
 * <p>A file has the same content print only when its last modification time, size
 * and binary content (verified by a sparse hash) are all exactly the same. A file
 * that is a binary copy but has a different last modification time will still have
 * a different content print.
 *
 * <p>A directory conversely has the same content print when its URI matches. Its
 * last modification time and hash are irrelevant.
 *
 * <p>Content prints are primarily intended to associate state with that should not be
 * lost when a file is renamed.
 */
public class ContentPrint {
  private final ContentID id;  // an identifier that never changes for this particular ContentPrint
  private final Long size;
  private final byte[] hash;
  private final long lastModificationTime;
  private final Instant signatureCreationTime;

  public ContentPrint(ContentID id, Long size, long lastModificationTime, byte[] hash, Instant signatureCreationTime) {
    if(id == null) {
      throw new IllegalArgumentException("id cannot be null");
    }
    if(hash == null) {
      throw new IllegalArgumentException("hash cannot be null");
    }
    if(signatureCreationTime == null) {
      throw new IllegalArgumentException("signatureCreationTime cannot be null");
    }

    this.id = id;
    this.size = size;
    this.lastModificationTime = lastModificationTime;
    this.hash = hash;
    this.signatureCreationTime = signatureCreationTime;
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

  /**
   * The last modification time in milliseconds since the epoch.
   *
   * @return last modification time in milliseconds since the epoch
   */
  public long getLastModificationTime() {
    return lastModificationTime;
  }

  public Instant getSignatureCreationTime() {
    return signatureCreationTime;
  }

  @Override
  public String toString() {
    return "ContentPrint(" + id + ",size=" + size + ",modTime=" + lastModificationTime + ",hash=" + Arrays.toString(hash) + ")";
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, size, Arrays.hashCode(hash), lastModificationTime, signatureCreationTime);
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

    if(!Objects.equals(signatureCreationTime, other.signatureCreationTime)) {
      return false;
    }

    if(!Objects.equals(size, other.size)) {
      return false;
    }

    return true;
  }
}

package hs.mediasystem.ext.basicmediatypes;

import hs.mediasystem.util.ByteArrays;
import hs.mediasystem.util.StringURI;

import java.util.Arrays;
import java.util.Objects;

public class StreamPrint {
  private final StringURI uri;
  private final Long size;
  private final byte[] hash;
  private final Long openSubtitleHash;
  private final long lastModificationTime;
  private final String identifier;

  private StreamPrint(StringURI uri, String identifier, Long size, long lastModificationTime, byte[] hash, Long openSubtitleHash) {
    if(uri == null) {
      throw new IllegalArgumentException("uri cannot be null");
    }
    if(identifier == null || identifier.isEmpty()) {
      throw new IllegalArgumentException("identifier cannot be null or empty");
    }

    this.uri = uri;
    this.identifier = identifier;
    this.size = size;
    this.lastModificationTime = lastModificationTime;
    this.hash = hash;
    this.openSubtitleHash = openSubtitleHash;
  }

  public StreamPrint(StringURI uri, Long size, long lastModificationTime, byte[] hash, Long openSubtitleHash) {
    this(uri, ByteArrays.toHex(hash) + "-" + size + "-" + lastModificationTime, size, lastModificationTime, hash, openSubtitleHash);
  }

  public String getIdentifier() {
    return identifier;
  }

  public StringURI getUri() {
    return uri;
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
  public int hashCode() {
    return Objects.hash(uri, size, hash, openSubtitleHash, lastModificationTime, identifier);
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }

    StreamPrint other = (StreamPrint)obj;

    if(!Arrays.equals(hash, other.hash)) {
      return false;
    }
    if(!identifier.equals(other.identifier)) {
      return false;
    }
    if(lastModificationTime != other.lastModificationTime) {
      return false;
    }
    if(openSubtitleHash == null) {
      if(other.openSubtitleHash != null) {
        return false;
      }
    }
    else if(!openSubtitleHash.equals(other.openSubtitleHash)) {
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
    if(!uri.equals(other.uri)) {
      return false;
    }

    return true;
  }
}

package hs.mediasystem.db.contentprints;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

@Table(name = "content_prints")
public class ContentPrintRecord extends DatabaseObject {

  @Id @Column
  private Integer id;

  @Column
  private byte[] hash;

  @Column
  private Long size;

  @Column(name = "modtime")
  private long lastModificationTime;  // in milliseconds since epoch

  @Column
  private Long lastSeenTime;  // in milliseconds since epoch

  @Column(name = "creation_ms")
  private long creationMillis;  // signature creation millis

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public long getLastModificationTime() {
    return lastModificationTime;
  }

  public void setLastModificationTime(long lastModificationTime) {
    this.lastModificationTime = lastModificationTime;
  }

  public Long getLastSeenTime() {
    return lastSeenTime;
  }

  public void setLastSeenTime(Long lastSeenTime) {
    this.lastSeenTime = lastSeenTime;
  }

  public long getCreationMillis() {
    return creationMillis;
  }

  public void setCreationMillis(long creationMillis) {
    this.creationMillis = creationMillis;
  }
}

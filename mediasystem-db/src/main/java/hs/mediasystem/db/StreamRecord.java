package hs.mediasystem.db;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "streams")
public class StreamRecord {

  @Id(generated = false)
  @Column(name = "stream_id")
  private int streamId;

  @Column(name = "scanner_id")
  private int importSourceId;

  @Column(name = "creation_ms")
  private long creationMillis;

  @Column(name = "lastenrichtime")
  private Long lastEnrichTime;  // in seconds since epoch

  @Column(name = "nextenrichtime")
  private Long nextEnrichTime;  // in seconds since epoch

  @Column
  private byte[] json;

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }

  public int getImportSourceId() {
    return importSourceId;
  }

  public void setImportSourceId(int importSourceId) {
    this.importSourceId = importSourceId;
  }

  public long getCreationMillis() {
    return creationMillis;
  }

  public void setCreationMillis(long creationMillis) {
    this.creationMillis = creationMillis;
  }

  public Long getLastEnrichTime() {
    return lastEnrichTime;
  }

  public void setLastEnrichTime(Long lastEnrichTime) {
    this.lastEnrichTime = lastEnrichTime;
  }

  public Long getNextEnrichTime() {
    return nextEnrichTime;
  }

  public void setNextEnrichTime(Long nextEnrichTime) {
    this.nextEnrichTime = nextEnrichTime;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}

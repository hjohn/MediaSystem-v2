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
  private int scannerId;

  @Column(name = "lastenrichtime")
  private Long lastEnrichTime;

  @Column(name = "nextenrichtime")
  private Long nextEnrichTime;

  @Column
  private byte[] json;

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }

  public int getScannerId() {
    return scannerId;
  }

  public void setScannerId(int scannerId) {
    this.scannerId = scannerId;
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

package hs.mediasystem.db;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "streamstate")
public class StreamStateRecord {

  @Id(generated = false)
  @Column(name = {"hash", "size", "modtime"})
  private StreamStateRecordId id;

  @Column
  private byte[] json;

  public StreamStateRecordId getId() {
    return id;
  }

  public void setId(StreamStateRecordId id) {
    this.id = id;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}

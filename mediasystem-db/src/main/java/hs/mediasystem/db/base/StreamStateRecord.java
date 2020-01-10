package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "streamstate")
public class StreamStateRecord {

  @Id(generated = false)
  @Column(name = "stream_id")
  private int streamId;

  @Column
  private byte[] json;

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}

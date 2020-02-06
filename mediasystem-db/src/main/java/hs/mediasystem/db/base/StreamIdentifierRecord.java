package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Table;

@Table(name = "stream_identifier")
public class StreamIdentifierRecord {

  @Column
  private String identifier;

  @Column(name = "stream_id")
  private int streamId;

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
}

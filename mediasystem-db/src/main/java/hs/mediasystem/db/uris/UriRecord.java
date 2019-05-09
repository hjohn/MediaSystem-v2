package hs.mediasystem.db.uris;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

@Table(name = "uris")
public class UriRecord extends DatabaseObject {

  @Id @Column
  private Integer id;

  @Column(name = "stream_id")
  private int streamId;

  @Column
  private String uri;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public int getStreamId() {
    return streamId;
  }

  public void setStreamId(int streamId) {
    this.streamId = streamId;
  }
}


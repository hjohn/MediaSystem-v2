package hs.mediasystem.db.extract;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

@Table(name = "stream_metadata")
public class StreamMetaDataRecord extends DatabaseObject {

  @Id(generated = false)
  @Column(name = "content_id")
  private int contentId;

  @Column(name = "modtime")
  private long lastModificationTime;

  @Column(name = "version")
  private int version;

  @Column
  private byte[] json;

  public int getContentId() {
    return contentId;
  }

  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  public long getLastModificationTime() {
    return lastModificationTime;
  }

  public void setLastModificationTime(long lastModificationTime) {
    this.lastModificationTime = lastModificationTime;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}

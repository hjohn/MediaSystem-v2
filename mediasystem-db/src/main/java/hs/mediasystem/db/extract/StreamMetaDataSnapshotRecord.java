package hs.mediasystem.db.extract;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

@Table(name = "stream_metadata_snapshots")
public class StreamMetaDataSnapshotRecord extends DatabaseObject {

  @Id(generated = false)
  @Column(name = "content_id")
  private int contentId;

  @Column
  private int index;

  @Column
  private byte[] image;

  public int getContentId() {
    return contentId;
  }

  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}

package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Table;

@Table(name = "stream_identifier")
public class StreamIdentifierRecord {

  @Column
  private String identifier;

  @Column(name = "content_id")
  private int contentId;

  public int getContentId() {
    return contentId;
  }

  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
}

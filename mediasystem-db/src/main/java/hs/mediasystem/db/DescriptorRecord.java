package hs.mediasystem.db;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "descriptors")
public class DescriptorRecord {

  @Id(generated = false)
  @Column
  private String identifier;

  @Column(name = "lastusedtime")
  private long lastUsedTime;

  @Column
  private byte[] json;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public long getLastUsedTime() {
    return lastUsedTime;
  }

  public void setLastUsedTime(long lastUsedTime) {
    this.lastUsedTime = lastUsedTime;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}

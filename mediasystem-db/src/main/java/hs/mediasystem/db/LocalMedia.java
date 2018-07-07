package hs.mediasystem.db;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

import java.time.LocalDateTime;

@Table(name = "localmedia")
public class LocalMedia extends DatabaseObject {

  @Id(generated = false) @Column
  private String id;

  @Column
  private LocalDateTime deleteTime;

  @Column
  private long scannerId;

  @Column
  private byte[] json;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getScannerId() {
    return scannerId;
  }

  public void setScannerId(long scannerId) {
    this.scannerId = scannerId;
  }

  public LocalDateTime getDeleteTime() {
    return deleteTime;
  }

  public void setDeleteTime(LocalDateTime deleteTime) {
    this.deleteTime = deleteTime;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}
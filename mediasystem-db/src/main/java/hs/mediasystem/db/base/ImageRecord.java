package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

import java.time.LocalDateTime;

@Table(name = "images")
public class ImageRecord extends DatabaseObject {

  @Id(generated = false)
  @Column
  private String url;

  @Column
  private String key;

  @Column
  private LocalDateTime creationTime;

  @Column
  private LocalDateTime accessTime;

  @Column
  private byte[] image;

  public ImageRecord(String url, String key, byte[] data) {
    this.url = url;
    this.creationTime = LocalDateTime.now();
    this.accessTime = creationTime;
    this.key = key;
    this.image = data;
  }

  public ImageRecord() {
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  public LocalDateTime getAccessTime() {
    return accessTime;
  }

  public void setAccessTime(LocalDateTime accessTime) {
    this.accessTime = accessTime;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}

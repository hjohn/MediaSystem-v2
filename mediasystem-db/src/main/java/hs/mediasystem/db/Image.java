package hs.mediasystem.db;

import hs.database.annotations.Column;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

import java.time.LocalDateTime;

@Table(name = "images")
public class Image extends DatabaseObject {

  @Column
  private String url;

  @Column
  private LocalDateTime creationTime;

  @Column
  private LocalDateTime accessTime;

  @Column
  private byte[] image;

  public Image(String url, byte[] data) {
    this.url = url;
    this.creationTime = LocalDateTime.now();
    this.accessTime = creationTime;
    this.image = data;
  }

  public Image() {
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

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }
}

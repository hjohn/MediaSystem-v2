package hs.mediasystem.db;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;
import hs.database.core.DatabaseObject;

@Table(name = "streamdata")
public class StreamData extends DatabaseObject {

  @Id @Column
  private Integer id;

  @Column
  private String url;

  @Column
  private byte[] hash;

  @Column
  private Long size;

  @Column(name = "modtime")
  private long lastModificationTime;

  @Column
  private byte[] json;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public byte[] getHash() {
    return hash;
  }

  public void setHash(byte[] hash) {
    this.hash = hash;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public long getLastModificationTime() {
    return lastModificationTime;
  }

  public void setLastModificationTime(long lastModificationTime) {
    this.lastModificationTime = lastModificationTime;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }
}
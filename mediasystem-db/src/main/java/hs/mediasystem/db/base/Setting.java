package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

import java.util.Date;

@Table(name = "settings")
public class Setting {
  public enum PersistLevel {PERMANENT, TEMPORARY, SESSION}

  @Id
  private Integer id;

  @Column
  private String system;

  @Column
  private PersistLevel persistLevel;

  @Column(name = "name")
  private String key;

  @Column
  private String value;

  @Column
  private Date lastUpdated;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getSystem() {
    return system;
  }

  public void setSystem(String system) {
    this.system = system;
  }

  public PersistLevel getPersistLevel() {
    return persistLevel;
  }

  public void setPersistLevel(PersistLevel persistLevel) {
    this.persistLevel = persistLevel;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  public String toString() {
    return "Setting[id=" + id + "; system='" + system + "'; persistLevel=" + persistLevel + "; key='" + key + "'; value='" + getValue() +"']";
  }
}

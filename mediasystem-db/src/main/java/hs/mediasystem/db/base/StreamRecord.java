package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

import java.util.List;

@Table(name = "streams")
public class StreamRecord {

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "parent_id")
  private Integer parentId;

  @Column(name = "content_id")
  private int contentId;

  @Column(name = "scanner_id")
  private int importSourceId;

  @Column(name = "name")
  private String name;

  @Column(name = "creation_ms")
  private long creationMillis;

  @Column(name = "lastenrichtime")
  private Long lastEnrichTime;  // in seconds since epoch

  @Column(name = "nextenrichtime")
  private Long nextEnrichTime;  // in seconds since epoch

  @Column
  private byte[] json;

  private List<String> identifiers;  // handled separately with m2m table

  @Column(name = "match_type")
  private String matchType;

  @Column(name = "match_ms")
  private Long matchMillis;

  @Column(name = "match_accuracy")
  private Float matchAccuracy;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getContentId() {
    return contentId;
  }

  public void setContentId(int contentId) {
    this.contentId = contentId;
  }

  public int getImportSourceId() {
    return importSourceId;
  }

  public void setImportSourceId(int importSourceId) {
    this.importSourceId = importSourceId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getCreationMillis() {
    return creationMillis;
  }

  public void setCreationMillis(long creationMillis) {
    this.creationMillis = creationMillis;
  }

  public Long getLastEnrichTime() {
    return lastEnrichTime;
  }

  public void setLastEnrichTime(Long lastEnrichTime) {
    this.lastEnrichTime = lastEnrichTime;
  }

  public Long getNextEnrichTime() {
    return nextEnrichTime;
  }

  public void setNextEnrichTime(Long nextEnrichTime) {
    this.nextEnrichTime = nextEnrichTime;
  }

  public byte[] getJson() {
    return json;
  }

  public void setJson(byte[] json) {
    this.json = json;
  }

  public Integer getParentId() {
    return parentId;
  }

  public void setParentId(Integer parentId) {
    this.parentId = parentId;
  }

  public List<String> getIdentifiers() {
    return identifiers;
  }

  public void setIdentifiers(List<String> identifiers) {
    this.identifiers = identifiers;
  }

  public Float getMatchAccuracy() {
    return matchAccuracy;
  }

  public void setMatchAccuracy(Float matchAccuracy) {
    this.matchAccuracy = matchAccuracy;
  }

  public Long getMatchMillis() {
    return matchMillis;
  }

  public void setMatchMillis(Long matchMillis) {
    this.matchMillis = matchMillis;
  }

  public String getMatchType() {
    return matchType;
  }

  public void setMatchType(String matchType) {
    this.matchType = matchType;
  }
}

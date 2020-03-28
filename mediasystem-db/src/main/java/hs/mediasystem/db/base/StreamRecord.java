package hs.mediasystem.db.base;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

import java.util.List;

@Table(name = "streams")
public class StreamRecord {

  @Id(generated = false)
  @Column(name = "content_id")
  private int contentId;

  @Column(name = "scanner_id")
  private int importSourceId;

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

  @Column(name = "parent_content_id")
  private Integer parentContentId;

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

  public Integer getParentContentId() {
    return parentContentId;
  }

  public void setParentContentId(Integer parentContentId) {
    this.parentContentId = parentContentId;
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

package hs.mediasystem.db.base;

import java.util.Date;

public record Setting(Integer id, String system, PersistLevel persistLevel, String name, String value, Date lastUpdated) {
  public enum PersistLevel {PERMANENT, TEMPORARY, SESSION}
}

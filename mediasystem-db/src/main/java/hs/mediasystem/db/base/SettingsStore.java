package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.database.core.DatabaseException;
import hs.mediasystem.db.base.Setting.PersistLevel;
import hs.mediasystem.util.Throwables;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SettingsStore {
  private static final String SEPARATOR = "/::/";
  private static final Logger LOGGER = Logger.getLogger(SettingsStore.class.getName());
  private static final Pattern INT_PATTERN = Pattern.compile("-?\\d+");

  @Inject private Database database;

  private final Map<String, String> cache = new HashMap<>();
  private final Set<String> dirtyKeys = new HashSet<>();

  @PostConstruct
  private void postConstruct() {
    Thread thread = new Thread(() -> {
      try {
        for(;;) {
          Thread.sleep(10000);

          Set<String> copiedDirtyKeys;

          synchronized(dirtyKeys) {
            if(dirtyKeys.isEmpty()) {
              continue;
            }

            copiedDirtyKeys = new HashSet<>(dirtyKeys);
            dirtyKeys.clear();
          }

          try(Transaction tx = database.beginTransaction()) {
            for(String key : copiedDirtyKeys) {
              int index = key.indexOf(SEPARATOR);
              String system = key.substring(0, index);
              String name = key.substring(index + SEPARATOR.length());
              Setting setting = tx.selectUnique(Setting.class, "system = ? AND name = ?", system, name);

              if(setting == null) {
                setting = new Setting();

                setting.setSystem(system);
                setting.setKey(name);
                setting.setPersistLevel(PersistLevel.PERMANENT);
              }

              setting.setValue(cache.get(key));
              setting.setLastUpdated(new Date());

              tx.merge(setting);
            }

            tx.commit();
          }
          catch(DatabaseException e) {
            LOGGER.warning("Unable to store modified settings: " + Throwables.formatAsOneLine(e));
          }
        }
      }
      catch(InterruptedException e) {
        throw new IllegalStateException(e);
      }
    });

    thread.setName("SettingStorePersister");
    thread.setDaemon(true);
    thread.start();
  }

  public String getSetting(String system, String name) {
    String key = key(system, name);
    String value = cache.get(key);

    if(value != null) {
      LOGGER.fine("Retrieved cached value for '" + key + "': " + value);

      return value;
    }

    try(Transaction tx = database.beginReadOnlyTransaction()) {
      Setting setting = tx.selectUnique(Setting.class, "system = ? AND name = ?", system, name);

      value = setting == null ? null : setting.getValue();
      cache.put(key, value);

      LOGGER.fine("Retrieved database value for '" + key + "': " + value);

      return value;
    }
  }

  public String getSettingOrDefault(String system, String name, String defaultValue) {
    String value = getSetting(system, name);

    return value == null ? defaultValue : value;
  }

  public int getIntSettingOrDefault(String system, String name, int defaultValue, int min, int max) {
    String value = getSetting(system, name);

    if(value != null && INT_PATTERN.matcher(value).matches()) {
      return Math.min(max, Math.max(min, Integer.parseInt(value)));
    }

    return defaultValue;
  }

  public void storeSetting(String system, String name, String value) {
    String key = key(system, name);

    LOGGER.fine("Stored value for '" + key + "': " + value);

    cache.put(key, value);

    synchronized(dirtyKeys) {
      dirtyKeys.add(key);
    }
  }

  public void storeIntSetting(String system, String name, int value) {
    storeSetting(system, name, "" + value);
  }

  private static String key(String system, String name) {
    return system + SEPARATOR + name;
  }
}

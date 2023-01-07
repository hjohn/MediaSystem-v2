package hs.mediasystem.db.base;

import hs.database.core.Database;
import hs.database.core.Database.Transaction;
import hs.database.core.DatabaseException;
import hs.mediasystem.db.base.Setting.PersistLevel;
import hs.mediasystem.util.concurrent.NamedThreadFactory;
import hs.mediasystem.util.exception.Throwables;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SettingsStore {
  private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("SettingStorePersister", true));
  private static final String SEPARATOR = "/::/";
  private static final Logger LOGGER = Logger.getLogger(SettingsStore.class.getName());
  private static final Pattern INT_PATTERN = Pattern.compile("-?\\d+");

  @Inject private Database database;

  private final Map<String, String> cache = new HashMap<>();
  private final Map<String, String> dirtyEntries = new HashMap<>();

  @PostConstruct
  private void postConstruct() {
    EXECUTOR.scheduleWithFixedDelay(this::storeDirtyKeys, 10, 10, TimeUnit.SECONDS);
  }

  private void storeDirtyKeys() {
    Map<String, String> copiedDirtyEntries;

    synchronized(dirtyEntries) {
      if(dirtyEntries.isEmpty()) {
        return;
      }

      copiedDirtyEntries = new HashMap<>(dirtyEntries);
      dirtyEntries.clear();
    }

    try(Transaction tx = database.beginTransaction()) {
      Date now = new Date();

      for(Map.Entry<String, String> entry : copiedDirtyEntries.entrySet()) {
        String key = entry.getKey();
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

        setting.setValue(entry.getValue());
        setting.setLastUpdated(now);

        tx.merge(setting);
      }

      tx.commit();

      LOGGER.fine("Stored settings: " + copiedDirtyEntries);
    }
    catch(DatabaseException e) {
      LOGGER.warning("Unable to store modified settings: " + Throwables.formatAsOneLine(e));
    }
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

    cache.put(key, value);

    synchronized(dirtyEntries) {
      dirtyEntries.put(key, value);
    }
  }

  public void storeIntSetting(String system, String name, int value) {
    storeSetting(system, name, "" + value);
  }

  private static String key(String system, String name) {
    return system + SEPARATOR + name;
  }
}

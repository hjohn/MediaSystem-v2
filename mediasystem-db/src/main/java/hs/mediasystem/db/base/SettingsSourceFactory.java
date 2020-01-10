package hs.mediasystem.db.base;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SettingsSourceFactory {
  @Inject private SettingsStore settingsStore;

  public SettingsSource of(String system) {
    return new SettingsSource(system);
  }

  public class SettingsSource {
    private final String system;

    private SettingsSource(String system) {
      this.system = system;
    }

    public String getSetting(String name) {
      return settingsStore.getSetting(system, name);
    }

    public String getSettingOrDefault(String name, String defaultValue) {
      return settingsStore.getSettingOrDefault(system, name, defaultValue);
    }

    public int getIntSettingOrDefault(String name, int defaultValue, int min, int max) {
      return settingsStore.getIntSettingOrDefault(system, name, defaultValue, min, max);
    }

    public void storeSetting(String name, String value) {
      settingsStore.storeSetting(system, name, value);
    }

    public void storeIntSetting(String name, int value) {
      settingsStore.storeIntSetting(system, name, value);
    }
  }
}

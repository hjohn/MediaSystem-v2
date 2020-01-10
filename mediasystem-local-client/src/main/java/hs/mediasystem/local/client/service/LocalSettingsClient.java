package hs.mediasystem.local.client.service;

import hs.mediasystem.db.base.SettingsSourceFactory;
import hs.mediasystem.ui.api.SettingsClient;
import hs.mediasystem.ui.api.domain.SettingsSource;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalSettingsClient implements SettingsClient {
  @Inject private SettingsSourceFactory factory;

  @Override
  public SettingsSource of(String system) {
    return toSettingsSource(factory.of(system));
  }

  private static SettingsSource toSettingsSource(hs.mediasystem.db.base.SettingsSourceFactory.SettingsSource ss) {
    return new SettingsSource() {

      @Override
      public String getSetting(String name) {
        return ss.getSetting(name);
      }

      @Override
      public String getSettingOrDefault(String name, String defaultValue) {
        return ss.getSettingOrDefault(name, defaultValue);
      }

      @Override
      public int getIntSettingOrDefault(String name, int defaultValue, int min, int max) {
        return ss.getIntSettingOrDefault(name, defaultValue, min, max);
      }

      @Override
      public void storeSetting(String name, String value) {
        ss.storeSetting(name, value);
      }

      @Override
      public void storeIntSetting(String name, int value) {
        ss.storeIntSetting(name, value);
      }
    };
  }
}

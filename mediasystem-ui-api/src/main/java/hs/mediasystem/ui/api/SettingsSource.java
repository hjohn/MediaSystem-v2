package hs.mediasystem.ui.api;

public interface SettingsSource {
  String getSetting(String name);
  String getSettingOrDefault(String name, String defaultValue);
  int getIntSettingOrDefault(String name, int defaultValue, int min, int max);
  void storeSetting(String name, String value);
  void storeIntSetting(String name, int value);
}

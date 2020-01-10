package hs.mediasystem.ui.api;

import hs.mediasystem.ui.api.domain.SettingsSource;

public interface SettingsClient {
  SettingsSource of(String key);
}

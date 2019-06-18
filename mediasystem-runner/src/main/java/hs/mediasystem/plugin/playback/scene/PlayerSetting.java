package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.PlayerFactory;
import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.util.ini.Ini;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class PlayerSetting implements Supplier<PlayerPresentation> {
  @Inject private Provider<List<PlayerFactory>> playerFactoriesProvider;
  @Inject private Ini ini;

  @Override
  public PlayerPresentation get() {
    String factoryClassName = ini.getSection("general").getDefault("player.factoryClass", "hs.mediasystem.ext.vlc.NativeWindowVLCPlayerFactory");

    return playerFactoriesProvider.get().stream()
      .filter(f -> f.getClass().getName().equals(factoryClassName))
      .findFirst()
      .map(f -> f.create(ini))
      .orElse(null);
  }
}

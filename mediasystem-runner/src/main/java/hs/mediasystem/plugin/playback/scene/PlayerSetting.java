package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.ui.api.player.PlayerFactory;
import hs.mediasystem.ui.api.player.PlayerPresentation;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class PlayerSetting implements Supplier<PlayerPresentation> {
  @Inject private Provider<List<PlayerFactory>> playerFactoriesProvider;
  @Inject @Named("general.player.factoryClass") private String factoryClassName;

  public Optional<PlayerFactory> getPlayerFactory() {
    return playerFactoriesProvider.get().stream()
      .filter(f -> f.getClass().getName().equals(factoryClassName))
      .findFirst();
  }

  @Override
  public PlayerPresentation get() {
    return getPlayerFactory()
      .map(f -> f.create())
      .orElse(null);
  }
}

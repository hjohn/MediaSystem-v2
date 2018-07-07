package hs.mediasystem.plugin.playback.scene;

import hs.mediasystem.domain.PlayerPresentation;
import hs.mediasystem.domain.PlayerFactory;
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
    return playerFactoriesProvider.get().get(0).create(ini);

//
//    injector.register(new Provider<Player>() {
//      private Player player;
//
//      @Override
//      public Player get() {
//        if(player == null) {
//          try {
//            player = selectedPlayerFactory.get().create(INI);
//          }
//          catch(Exception e) {
//            LOGGER.log(Level.WARNING, "Unable to create player using factory: " + selectedPlayerFactory.get(), e);
//          }
//        }
//
//        return player;
//      }
//    });
//
//    return new SceneLocation<>("RootMenu", null);  // TODO needs to be configurable
  }
}

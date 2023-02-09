package hs.mediasystem.runner;

import hs.mediasystem.runner.util.DebugSceneFX;
import hs.mediasystem.runner.util.FXSceneManager;
import hs.mediasystem.runner.util.SceneManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.int4.dirk.annotations.Produces;

@Singleton
public class Configuration {
  @Inject @Nullable @Named("general.screen") public Long screenNumber = 0L;
  @Inject @Nullable @Named("general.alwaysOnTop") public Boolean alwaysOnTop = false;
  @Inject @Nullable @Named("general.debug.dumpUnreferencedNodes") public Integer dumpUnreferencedNodes = 0;

  @Produces
  @Singleton
  SceneManager sceneManager() {
    FXSceneManager sceneManager = new FXSceneManager("MediaSystem", screenNumber.intValue(), alwaysOnTop);

    if(dumpUnreferencedNodes > 0) {
      DebugSceneFX.monitor(sceneManager.getScene(), dumpUnreferencedNodes * 1000L);
    }

    return sceneManager;
  }
}

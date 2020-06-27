package hs.mediasystem.runner;

import hs.ddif.core.Produces;
import hs.mediasystem.runner.util.FXSceneManager;
import hs.mediasystem.runner.util.SceneManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

public class Configuration {
  @Inject @Nullable @Named("general.screen") public Long screenNumber = 0L;
  @Inject @Nullable @Named("general.alwaysOnTop") public Boolean alwaysOnTop = false;

  @Produces
  @Singleton
  SceneManager sceneManager() {
    return new FXSceneManager("MediaSystem", screenNumber.intValue(), alwaysOnTop);
  }
}

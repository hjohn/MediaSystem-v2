package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.StartupPresentationProvider.Plugin;

import javax.inject.Singleton;

public class HomePresentation implements Presentation {

  @Singleton
  public static class Factory implements Plugin {
    @Override
    public HomePresentation create() {
      return new HomePresentation();
    }
  }
}

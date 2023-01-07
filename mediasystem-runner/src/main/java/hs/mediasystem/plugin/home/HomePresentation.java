package hs.mediasystem.plugin.home;

import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.runner.StartupPresentationProvider.Plugin;
import hs.mediasystem.util.domain.Tuple;
import hs.mediasystem.util.domain.Tuple.Tuple2;

import javax.inject.Singleton;

import org.reactfx.value.Var;

public class HomePresentation implements Presentation {
  public final Var<Tuple2<String, Integer>> selectedItem = Var.newSimpleVar(Tuple.of("Home", 0));

  @Singleton
  public static class Factory implements Plugin {
    @Override
    public HomePresentation create() {
      return new HomePresentation();
    }
  }
}
